package failchat.core.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import failchat.core.chat.StatusMessageMode.NOWHERE
import failchat.core.chat.handlers.IgnoreFilter
import failchat.core.chat.handlers.ImageLinkHandler
import failchat.core.chat.handlers.LinkHandler
import failchat.core.emoticon.Emoticon
import failchat.core.ws.server.WsServer
import failchat.gui.StatusMessageModeConverter
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChatMessageSender(
        private val wsServer: WsServer,
        private val config: Configuration,
        ignoreFilter: IgnoreFilter,
        imageLinkHandler: ImageLinkHandler,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ChatMessageSender::class.java)
    }

    private val handlers: List<MessageHandler<ChatMessage>> = listOf(
            LinkHandler(),
            imageLinkHandler
    )
    private val filters: List<MessageFilter<ChatMessage>> = listOf(ignoreFilter)
    private val statusMessagesModeConverter = StatusMessageModeConverter()


    fun send(message: ChatMessage) {
        // Apply filters and handlers
        filters.forEach {
            if (it.filterMessage(message)) return
        }
        handlers.forEach { it.handleMessage(message) }


        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "message")
            with("content").apply {
                put("id", message.id)
                put("origin", message.origin.name)
                put("author", message.author)
                put("text", message.text)
                put("timestamp", message.timestamp.toEpochMilli())
                put("highlighted", message.highlighted)
            }
        }

        val elementsArray: ArrayNode = messageNode
                .with("content")
                .withArray("elements")

        message.elements.forEach { element ->
            when (element) {
                is Emoticon -> elementsArray.addObject()
                        .put("type", "emoticon") //todo enum
                        .put("origin", element.origin.name)
                        .put("code", element.code)
                        .put("url", element.url)

                is Link -> elementsArray.addObject()
                        .put("type", "link")
                        .put("domain", element.domain)
                        .put("fullUrl", element.fullUrl)
                        .put("shortUrl", element.shortUrl)

                is Image -> elementsArray.addObject()
                        .put("type", "image")
                        .put("url", element.link.fullUrl)

                else -> {
                    log.error("Unknown element type: {}", element.javaClass.name)
                }
            }
        }

        wsServer.send(messageNode.toString())
    }

    fun send(message: StatusMessage) {
        val mode = statusMessagesModeConverter.fromString(config.getString("status-message-mode"))
        if (mode == NOWHERE) return

        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "origin-status")
            putObject("content").apply {
                put("origin", message.origin.name)
                put("status", message.status.lowerCaseString)
                put("timestamp", message.timestamp.toEpochMilli())
                put("mode", mode.lowerCaseString) //todo don't send mode here
            }
        }

        wsServer.send(messageNode.toString())
    }

}
