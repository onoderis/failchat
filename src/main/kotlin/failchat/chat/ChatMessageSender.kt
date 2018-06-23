package failchat.chat

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.chat.StatusMessageMode.NOWHERE
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.chat.handlers.LinkHandler
import failchat.emoticon.Emoticon
import failchat.gui.StatusMessageModeConverter
import failchat.viewers.COUNTABLE_ORIGINS
import failchat.ws.server.WsServer
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChatMessageSender(
        private val wsServer: WsServer,
        private val config: Configuration,
        ignoreFilter: IgnoreFilter,
        imageLinkHandler: ImageLinkHandler
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ChatMessageSender::class.java)
    }

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
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


        val messageNode = nodeFactory.objectNode().apply {
            put("type", "message")
            with("content").apply {
                put("id", message.id)
                put("origin", message.origin.commonName)
                putObject("author").apply {
                    put("name", message.author.name)
                    put("id", message.author.id)
                }
                put("text", message.text)
                put("timestamp", message.timestamp.toEpochMilli())
                put("highlighted", message.highlighted)
                putArray("badges").also {
                    serializeBadges(message, it)
                }
            }
        }

        val elementsArray: ArrayNode = messageNode
                .with("content")
                .withArray("elements")

        message.elements.forEach { element ->
            when (element) {
                is Emoticon -> elementsArray.addObject()
                        .put("type", "emoticon") //todo enum
                        .put("code", element.code)
                        .put("url", element.url)
                        .put("format", element.format.jsonValue)

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

    private fun serializeBadges(message: ChatMessage, badgesArrayNode: ArrayNode) {
        message.badges.forEach { badge ->
            badgesArrayNode.addObject().apply {
                @Suppress("REDUNDANT_ELSE_IN_WHEN")
                when (badge) {
                    is ImageBadge -> {
                        put("type", "image")
                        put("format", badge.format.jsonValue)
                        put("url", badge.url)
                        put("description", badge.description)
                    }
                    is CharacterBadge -> {
                        put("type", "character")
                        put("htmlEntity", badge.characterEntity)
                        put("color", badge.color)
                    }
                    else -> throw IllegalStateException()
                }
            }
        }
    }

    fun send(message: StatusMessage) {
        //todo optimize
        val mode = statusMessagesModeConverter.fromString(config.getString("status-message-mode"))
        if (mode == NOWHERE) return

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "origin-status")
            putObject("content").apply {
                put("origin", message.origin.commonName)
                put("status", message.status.jsonValue)
                put("timestamp", message.timestamp.toEpochMilli())
            }
        }

        wsServer.send(messageNode.toString())
    }

    /** Send client configuration to all clients. */
    fun sendClientConfiguration() {
        val mode = statusMessagesModeConverter.fromString(config.getString("status-message-mode"))

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "client-configuration")
            putObject("content").apply {
                put("statusMessageMode", mode.jsonValue)
                put("showViewersCount", config.getBoolean("show-viewers"))
                put("nativeClientBgColor", config.getString("background-color.native"))
                put("externalClientBgColor", config.getString("background-color.external"))
                put("showOriginBadges", config.getBoolean("show-origin-badges"))
                put("showUserBadges", config.getBoolean("show-user-badges"))
                put("zoomPercent", config.getInt("zoom-percent"))
                putObject("enabledOrigins").apply {
                    COUNTABLE_ORIGINS.forEach { origin ->
                        put(origin.commonName, config.getBoolean("${origin.commonName}.enabled"))
                    }
                }
            }
        }

        wsServer.send(messageNode.toString())
    }

}
