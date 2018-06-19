package failchat.cybergame

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.EvictingQueue
import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.READY
import failchat.chat.ChatMessage
import failchat.chat.Elements
import failchat.chat.ImageFormat.RASTER
import failchat.chat.ImageFormat.VECTOR
import failchat.chat.Link
import failchat.chat.MessageElement
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.handlers.BraceEscaper
import failchat.chat.handlers.CommaHighlightHandler
import failchat.util.objectMapper
import failchat.util.synchronized
import failchat.util.urlPattern
import failchat.util.value
import failchat.util.withSuffix
import failchat.ws.client.WsClient
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

class CgChatClient(
        private val channelName: String,
        private val channelId: Long,
        wsUrl: String,
        private val emoticonUrlPrefix: String,
        private val messageIdGenerator: MessageIdGenerator
) : ChatClient<ChatMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CgChatClient::class.java)
        val acceptedMessageTypes: Set<String> = CgWsMessageType.values().mapTo(HashSet()) { it.jsonValue }
    }

    private val wsClient = CgWsClient(URI(wsUrl.withSuffix("/")))
    private val aStatus: AtomicReference<ChatClientStatus> = AtomicReference(READY)
    private val history = EvictingQueue.create<CgChatMessage>(50).synchronized()
    private val handlers: List<MessageHandler<CgChatMessage>> = listOf(
            //todo ElementLabelEscaper(),
            BraceEscaper(),
            CommaHighlightHandler(channelName)
    )

    override val origin = Origin.CYBERGAME
    override val status: ChatClientStatus = aStatus.value

    override var onChatMessage: ((ChatMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((ChatMessage) -> Unit)?  = null

    override fun start() = wsClient.start()

    override fun stop() = wsClient.stop()


    inner class CgWsClient(uri: URI) : WsClient(uri) {
        override fun onOpen(serverHandshake: ServerHandshake) {
            val connectMessage = objectMapper.createObjectNode().apply {
                put("type", "join")
                putObject("data").apply {
                    put("cid", channelId.toString())
                }
            }
            send(connectMessage.toString())
        }

        override fun onMessage(message: String) {
            val messageNode = objectMapper.readTree(message)

            val type: String = messageNode.get("type").textValue()
            if (!acceptedMessageTypes.contains(type)) {
                log.debug("Message with type '{}' ignored", type)
                return
            }

            val data: JsonNode = messageNode.get("data")

            when (type) {
                CgWsMessageType.STATE.jsonValue -> {
                    val state = data.get("state").intValue()
                    if (state == 2) {
                        onStatusMessage?.invoke(StatusMessage(origin, CONNECTED))
                    }
                }

                CgWsMessageType.MESSAGE.jsonValue -> {
                    val parsedMessage = parseChatMessage(data)

                    handlers.forEach {
                        it.handleMessage(parsedMessage)
                    }

                    history.add(parsedMessage)
                    onChatMessage?.invoke(parsedMessage)
                }

                CgWsMessageType.CLEAR.jsonValue -> {
                    val userId: String = data.get("message").get("uid").asText()
                    history
                            .filter { it.author.id == userId }
                            .forEach { onChatMessageDeleted?.invoke(it) }
                }

                else -> throw IllegalStateException("Unhandled type '$type'")
            }
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            log.info("Cybergame chat client disconnected. channel: '{}', channel id: {}", channelName, channelId)
        }

        override fun onReconnect() {
            log.info("Cybergame chat client disconnected, trying to reconnect. channel: '{}', channel id: {}", channelName, channelId)
            onStatusMessage?.invoke(StatusMessage(origin, DISCONNECTED))
        }

        override fun onError(e: Exception) {
            log.error("Cybergame chat client error. channel: '{}', channel id: {}", channelName, channelId, e)
        }
    }

    private fun parseChatMessage(messageDataNode: JsonNode): CgChatMessage {
        var elementNumber = 0

        val messageContentFolder = messageDataNode.get("message")
                .asSequence()
                .mapNotNull<JsonNode, MessagePart> {
                    val type = it.get("type").textValue()
                    when (type) {
                        "text" -> MessagePart.Text(it.get("text").asText())
                        "emote" -> {
                            val emoticonSuffix = it.get("image").textValue()
                            val format = if (emoticonSuffix.contains(".svg")) VECTOR else RASTER
                            val emoticonUrl = emoticonUrlPrefix + emoticonSuffix
                            val emoticon = CgEmoticon(emoticonSuffix, emoticonUrl, format)
                            MessagePart.Emoticon(elementNumber++, emoticon)
                        }
                        "link" -> {
                            val fullUrl = it.get("text").asText()
                            val matcher = urlPattern.matcher(fullUrl)
                            if (matcher.matches()) {
                                val parsedLink = Link(fullUrl, matcher.group(4), matcher.group(3))
                                MessagePart.Link(elementNumber++, parsedLink)
                            } else {
                                MessagePart.Text(fullUrl)
                            }
                        }
                        else -> {
                            log.warn("Unexpected message part ignored. type: '{}', message: {}", type, it)
                            null
                        }
                    }
                }
                .fold(MessageContentFolder()) { acc, part -> acc.fold(part) }

        val cgMessage = CgChatMessage(
                messageIdGenerator.generate(),
                Author(
                        messageDataNode.get("nickname").asText(),
                        messageDataNode.get("uid").asText()
                ),
                messageContentFolder.text
        )
        messageContentFolder.elements.forEach {
            cgMessage.addElement(it)
        }

        return cgMessage
    }

    private class MessageContentFolder {
        val text: String get() = textBuilder.toString()
        val elements: List<MessageElement> get() = mutableElements

        private val textBuilder = StringBuilder()
        private val mutableElements = mutableListOf<MessageElement>()

        fun fold(part: MessagePart): MessageContentFolder {
            when (part) {
                is MessagePart.Text -> textBuilder.append(part.text)
                is MessagePart.Emoticon -> {
                    textBuilder.append(Elements.label(part.elementNumber))
                    mutableElements.add(part.emoticon)
                }
                is MessagePart.Link -> {
                    textBuilder.append(Elements.label(part.elementNumber))
                    mutableElements.add(part.parsedLink)
                }
            }
            return this
        }
    }

    private sealed class MessagePart {
        class Text(val text: String) : MessagePart()
        class Emoticon(val elementNumber: Int, val emoticon: failchat.emoticon.Emoticon) : MessagePart()
        class Link(val elementNumber: Int, val parsedLink: failchat.chat.Link) : MessagePart()
    }

}