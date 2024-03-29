package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.Origin.GOODGAME
import failchat.chat.ChatClient
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.OFFLINE
import failchat.chat.ChatMessageHistory
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusUpdate
import failchat.chat.findFirstTyped
import failchat.chat.handlers.CommaHighlightHandler
import failchat.chat.handlers.ElementLabelEscaper
import failchat.ws.client.WsClient
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

class GgChatClient(
        private val channel: GgChannel,
        private val webSocketUri: String,
        private val messageIdGenerator: MessageIdGenerator,
        emoticonHandler: MessageHandler<GgMessage>,
        badgeHandler: GgBadgeHandler,
        private val history: ChatMessageHistory,
        override val callbacks: ChatClientCallbacks,
        private val objectMapper: ObjectMapper
) : ChatClient {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override val origin = Origin.GOODGAME
    override val status: ChatClientStatus get() = atomicStatus.get()


    private var wsClient: WsClient = GgWsClient(URI.create(webSocketUri))
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.READY)

    private val messageHandlers: List<MessageHandler<GgMessage>> = listOf(
            ElementLabelEscaper(),
            HtmlUrlCleaner(),
            emoticonHandler,
            CommaHighlightHandler(channel.name),
            badgeHandler,
            GgAuthorColorHandler()
    )


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

    private inner class GgWsClient(uri: URI) : WsClient(uri) {

        override fun onOpen(serverHandshake: ServerHandshake) {
            val joinMessage = objectMapper.createObjectNode().apply {
                put("type", "join")
                putObject("data").apply {
                    put("channel_id", channel.id.toString())
                    put("isHidden", false)
                }
            }
            atomicStatus.set(ChatClientStatus.CONNECTED)
            logger.info("Goodgame chat client connected to channel {}", channel.id)

            wsClient.send(joinMessage.toString())
            callbacks.onStatusUpdate(StatusUpdate(GOODGAME, CONNECTED))
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            logger.info("Goodgame chat client disconnected from channel {}", channel.id)
        }

        override fun onMessage(message: String) {
            val messageNode = objectMapper.readTree(message)
            val type = messageNode.get("type").asText()
            val data = messageNode.get("data")

            //todo log on unknown type
            when (type) {
                "message" -> handleUserMessage(data)
                "remove_message" -> handleModMessage(data)
            }
        }

        override fun onError(e: Exception) {
            logger.error("Goodgame chat client error", e)
        }

        override fun onReconnect() {
            logger.info("Goodgame chat client disconnected, trying to reconnect")
            callbacks.onStatusUpdate(StatusUpdate(GOODGAME, DISCONNECTED))
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

            callbacks.onChatMessage(ggMessage)
        }

        private fun handleModMessage(dataNode: JsonNode) {
            val idToRemove = dataNode.get("message_id").asText().toLong()

            val foundMessage = runBlocking {
                history.findFirstTyped<GgMessage> { it.ggId == idToRemove }
            }
            foundMessage?.let { callbacks.onChatMessageDeleted(it) }
        }
    }

}
