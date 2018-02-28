package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.EvictingQueue
import failchat.Origin
import failchat.Origin.GOODGAME
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.OFFLINE
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.handlers.CommaHighlightHandler
import failchat.chat.handlers.ElementLabelEscaper
import failchat.emoticon.EmoticonFinder
import failchat.twitch.TwitchChatClient
import failchat.util.synchronized
import failchat.util.whileNotNull
import failchat.viewers.ViewersCountLoader
import failchat.ws.client.WsClient
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

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
    }

    override val origin = Origin.GOODGAME
    override val status: ChatClientStatus get() = atomicStatus.get()

    override var onChatMessage: ((GgMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((GgMessage) -> Unit)? = null


    private var wsClient: WsClient = GgWsClient(URI.create(webSocketUri))
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.READY)

    private val messageHandlers: List<MessageHandler<GgMessage>> = listOf(
            ElementLabelEscaper(),
            HtmlUrlCleaner(),
            GgEmoticonHandler(emoticonFinder),
            CommaHighlightHandler(channelName)
    )

    private val history = EvictingQueue.create<GgMessage>(50).synchronized()
    private val viewersCountFutures: Queue<CompletableFuture<Int>> = ConcurrentLinkedQueue()


    override fun start() {
        //todo change to connecting
        if (atomicStatus.get() != ChatClientStatus.READY) {
            return
        }
        wsClient.start()
    }

    override fun stop() {
        atomicStatus.set(OFFLINE)
        wsClient.stop()
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
            atomicStatus.set(ChatClientStatus.CONNECTED)
            log.info("Goodgame chat client connected to channel {}", channelId)

            wsClient.send(joinMessage.toString())
            onStatusMessage?.invoke(StatusMessage(GOODGAME, CONNECTED))
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            log.info("Goodgame chat client disconnected from channel {}", channelId)
        }

        override fun onMessage(message: String) {
            val messageNode = objectMapper.readTree(message)
            val type = messageNode.get("type").asText()
            val data = messageNode.get("data")

            //todo log on unknown type
            when (type) {
                "message" -> handleUserMessage(data)
                "remove_message" -> handleModMessage(data)
                "viewers" -> handleViewersMessage(data)
            }
        }

        override fun onError(e: Exception) {
            log.error("Goodgame chat client error", e)
        }

        override fun onReconnect() {
            log.info("Goodgame chat client disconnected, trying to reconnect")
            onStatusMessage?.invoke(StatusMessage(GOODGAME, DISCONNECTED))
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

            history.add(ggMessage)
            onChatMessage?.invoke(ggMessage)
        }

        private fun handleModMessage(dataNode: JsonNode) {
            val idToRemove = dataNode.get("message_id").asText().toLong()

            val foundMessage = history.find { it.ggId == idToRemove }
            foundMessage?.let { onChatMessageDeleted?.invoke(it) }
        }

        private fun handleViewersMessage(dataNode: JsonNode) {
            val count = dataNode.get("count").asInt()
            whileNotNull({ viewersCountFutures.poll() }) {
                it.complete(count)
            }
        }
    }

}
