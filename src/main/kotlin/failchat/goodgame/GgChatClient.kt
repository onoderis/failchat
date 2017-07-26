package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.EvictingQueue
import failchat.core.Origin
import failchat.core.Origin.goodgame
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatClientStatus
import failchat.core.chat.ChatClientStatus.offline
import failchat.core.chat.InfoMessage
import failchat.core.chat.MessageHandler
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.handlers.CommonHighlightHandler
import failchat.core.chat.handlers.MessageObjectCleaner
import failchat.core.emoticon.EmoticonFinder
import failchat.core.viewers.ViewersCountLoader
import failchat.core.ws.client.WsClient
import failchat.twitch.TwitchChatClient
import failchat.utils.whileNotNull
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GgChatClient(
        private val channelName: String,
        private val channelId: Long,
        private val webSocketUri: String,
        private val messageIdGenerator: MessageIdGenerator,
        private val emoticonFinder: EmoticonFinder,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : ChatClient<GgMessage>, ViewersCountLoader {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchChatClient::class.java)
        const val historySize = 50
    }

    private var wsClient: WsClient = GgWsClient(URI.create(webSocketUri))
    private val _status: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.ready)

    private val messageHandlers: List<MessageHandler<GgMessage>> = listOf(
            MessageObjectCleaner(),
            HtmlUrlCleaner(),
            GgEmoticonHandler(emoticonFinder),
            CommonHighlightHandler(channelName)
    )

    private val history: Queue<GgMessage> = EvictingQueue.create(historySize)
    private val historyLock: Lock = ReentrantLock()
    private val viewersCountFutures: Queue<CompletableFuture<Int>> = ConcurrentLinkedQueue()

    private var chatMessageConsumer: ((GgMessage) -> Unit)? = null
    private var infoMessageConsumer: ((InfoMessage) -> Unit)? = null
    private var messageDeletedCallback: ((GgMessage) -> Unit)? = null


    override val origin = Origin.goodgame
    override val status: ChatClientStatus get() = _status.get()


    override fun start() {
        if (_status.get() != ChatClientStatus.ready) {
            return
        }
        wsClient.start()
    }

    override fun stop() {
        _status.set(offline)
        wsClient.stop()
    }

    override fun onChatMessage(consumer: (GgMessage) -> Unit) {
        chatMessageConsumer = consumer
    }

    override fun onInfoMessage(consumer: (InfoMessage) -> Unit) {
        infoMessageConsumer = consumer
    }

    override fun onChatMessageDeleted(operation: (GgMessage) -> Unit) {
        messageDeletedCallback = operation
    }

    override fun loadViewersCount(): CompletableFuture<Int> {
        /*
        * Undocumented api
        * request:  {"type":"get_all_viewers","data":{"channel":"21506"}}
        * response: {"type":"viewers","data":{"channel_id":"21506","count":173}}
        * Ответ приходит 1 раз. Если канал оффлайн - значение 0.
        * */
        val getAllViewersMessage = objectMapper.createObjectNode().apply {
            put("type", "get_all_viewers")
            putObject("data").apply {
                put("channel", channelId.toString())
            }
        }

        val countFuture = CompletableFuture<Int>()
        try {
            wsClient.send(getAllViewersMessage.toString())
        } catch (e: Exception) {
            countFuture.completeExceptionally(e)
        }
        if (!countFuture.isCompletedExceptionally) {
            viewersCountFutures.offer(countFuture)
        }
        return countFuture
    }

    private inner class GgWsClient(uri: URI) : WsClient(uri) {

        override fun onOpen(serverHandshake: ServerHandshake) {
            val joinMessage = objectMapper.createObjectNode().apply {
                put("type", "join")
                putObject("data").apply {
                    put("channel_id", channelId.toString())
                    put("isHidden", false)
                }
            }
            _status.set(ChatClientStatus.connected)
            log.info("Goodgame chat client connected to channel {}", channelId)

            wsClient.send(joinMessage.toString())
            infoMessageConsumer?.invoke(InfoMessage(messageIdGenerator.generate(), goodgame, "connected"))
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            log.info("Goodgame chat client disconnected from channel {}", channelId)
        }

        override fun onMessage(message: String) {
            val messageNode = objectMapper.readTree(message)
            val type = messageNode.get("type").asText()
            val data = messageNode.get("data")

            //todo log on unknown type
            when(type) {
                "message" -> handleUserMessage(data)
                "remove_message" -> handleModMessage(data)
                "viewers" -> handleViewersMessage(data)
            }
        }

        override fun onError(e: Exception) {
            log.warn("GgWsClient error", e)
        }

        override fun onReconnect() {
            log.info("Goodgame chat client disconnected, trying to reconnect")
            infoMessageConsumer?.invoke(InfoMessage(messageIdGenerator.generate(), goodgame, "disconnected"))
        }

        private fun handleUserMessage(dataNode: JsonNode) {
            val ggMessage = GgMessage(
                    id = messageIdGenerator.generate(),
                    ggId = dataNode.get("message_id").asText().toLong(),
                    author = dataNode.get("user_name").asText(),
                    text = dataNode.get("text").asText(),
                    authorHasPremium = dataNode.get("premium").asBoolean()
            )
            messageHandlers.forEach { it.handleMessage(ggMessage) }

            historyLock.withLock { history.add(ggMessage) }
            chatMessageConsumer?.invoke(ggMessage)
        }

        private fun handleModMessage(dataNode: JsonNode) {
            val idToRemove = dataNode.get("message_id").asText().toLong()

            val foundMessage = historyLock.withLock {
                history.find { it.ggId == idToRemove }
            }
            foundMessage?.let { messageDeletedCallback?.invoke(it) }
        }

        private fun handleViewersMessage(dataNode: JsonNode) {
            val count = dataNode.get("count").asInt()
            whileNotNull({ viewersCountFutures.poll() }) {
                it.complete(count)
            }
        }
    }

}
