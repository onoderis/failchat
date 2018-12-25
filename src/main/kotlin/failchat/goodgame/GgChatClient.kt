package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import failchat.Origin
import failchat.Origin.GOODGAME
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.OFFLINE
import failchat.chat.ChatMessageHistory
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.findFirstTyped
import failchat.chat.handlers.CommaHighlightHandler
import failchat.chat.handlers.ElementLabelEscaper
import failchat.exception.ChannelOfflineException
import failchat.util.completedFuture
import failchat.util.exceptionalFuture
import failchat.util.objectMapper
import failchat.viewers.ViewersCountLoader
import failchat.ws.client.WsClient
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class GgChatClient(
        private val channelName: String,
        private val channelId: Long,
        private val webSocketUri: String,
        private val messageIdGenerator: MessageIdGenerator,
        emoticonHandler: MessageHandler<GgMessage>,
        badgeHandler: GgBadgeHandler,
        private val history: ChatMessageHistory
) : ChatClient<GgMessage>, ViewersCountLoader {

    private companion object : KLogging()

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
            emoticonHandler,
            CommaHighlightHandler(channelName),
            badgeHandler
    )

    @Volatile
    private var viewersCount: Int? = null


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
        viewersCount?.let {
            return completedFuture(it)
        }
        return exceptionalFuture(ChannelOfflineException(Origin.GOODGAME, channelName))
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
            logger.info("Goodgame chat client connected to channel {}", channelId)

            wsClient.send(joinMessage.toString())
            onStatusMessage?.invoke(StatusMessage(GOODGAME, CONNECTED, messageIdGenerator.generate()))
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            logger.info("Goodgame chat client disconnected from channel {}", channelId)
        }

        override fun onMessage(message: String) {
            val messageNode = objectMapper.readTree(message)
            val type = messageNode.get("type").asText()
            val data = messageNode.get("data")

            //todo log on unknown type
            when (type) {
                "message" -> handleUserMessage(data)
                "remove_message" -> handleModMessage(data)
                "channel_counters" -> handleChannelCountersMessage(data)
            }
        }

        override fun onError(e: Exception) {
            logger.error("Goodgame chat client error", e)
        }

        override fun onReconnect() {
            logger.info("Goodgame chat client disconnected, trying to reconnect")
            onStatusMessage?.invoke(StatusMessage(GOODGAME, DISCONNECTED, messageIdGenerator.generate()))
        }

        private fun handleUserMessage(dataNode: JsonNode) {
            val subscriptionDuration = dataNode.get("resubs").fields().asSequence()
                    .map { (channelId, duration) -> channelId.toLong() to duration.intValue() }
                    .toMap(HashMap())
            val ggMessage = GgMessage(
                    id = messageIdGenerator.generate(),
                    ggId = dataNode.get("message_id").asText().toLong(),
                    author = dataNode.get("user_name").asText(),
                    text = dataNode.get("text").asText(),
                    authorHasPremium = dataNode.get("premium").asBoolean(),
                    subscriptionDuration = subscriptionDuration,
                    badgeName = dataNode.get("icon").textValue(),
                    authorColorName = dataNode.get("color").textValue(),
                    sponsorLevel = dataNode.get("payments").intValue(),
                    authorRights = dataNode.get("user_rights").intValue()
            )

            messageHandlers.forEach { it.handleMessage(ggMessage) }

            onChatMessage?.invoke(ggMessage)
        }

        private fun handleModMessage(dataNode: JsonNode) {
            val idToRemove = dataNode.get("message_id").asText().toLong()

            val foundMessage = runBlocking {
                history
                        .findFirstTyped<GgMessage> { it.ggId == idToRemove }
                        .await()
            }
            foundMessage?.let { onChatMessageDeleted?.invoke(it) }
        }

        private fun handleChannelCountersMessage(dataNode: JsonNode) {
            viewersCount = dataNode.get("clients_in_channel").asInt()
        }
    }

}
