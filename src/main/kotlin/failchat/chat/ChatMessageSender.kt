package failchat.chat

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.ConfigKeys
import failchat.chat.StatusMessageMode.NOWHERE
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import failchat.emoticon.Emoticon
import failchat.gui.StatusMessageModeConverter
import failchat.viewers.COUNTABLE_ORIGINS
import failchat.ws.server.WsFrameSender
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.apache.commons.configuration2.Configuration

//todo rename
class ChatMessageSender(
        private val wsFrameSender: WsFrameSender,
        private val config: Configuration,
        private val filters: List<MessageFilter<ChatMessage>>,
        private val handlers: List<MessageHandler<ChatMessage>>,
        private val history: ChatMessageHistory
) {

    private companion object : KLogging()

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
    private val statusMessagesModeConverter = StatusMessageModeConverter()


    fun send(message: ChatMessage) {
        // apply filters and handlers
        filters.forEach {
            if (it.filterMessage(message)) return
        }
        handlers.forEach { it.handleMessage(message) }

        runBlocking {
            history.add(message)
        }

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
                    logger.error("Unknown element type: {}", element.javaClass.name)
                }
            }
        }

        wsFrameSender.sendToAll(messageNode.toString())
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
        val mode = statusMessagesModeConverter.fromString(config.getString(ConfigKeys.statusMessageMode))
        if (mode == NOWHERE) return

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "origin-status")
            putObject("content").apply {
                put("id", message.id)
                put("origin", message.origin.commonName)
                put("status", message.status.jsonValue)
                put("timestamp", message.timestamp.toEpochMilli())
            }
        }

        wsFrameSender.sendToAll(messageNode.toString())
    }

    /** Send client configuration to all clients. */
    fun sendClientConfiguration() {
        val mode = statusMessagesModeConverter.fromString(config.getString(ConfigKeys.statusMessageMode))

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "client-configuration")
            putObject("content").apply {
                put("statusMessageMode", mode.jsonValue)
                put("showViewersCount", config.getBoolean(ConfigKeys.showViewers))
                put("nativeClientBgColor", config.getString(ConfigKeys.backgroundColor.native))
                put("externalClientBgColor", config.getString(ConfigKeys.backgroundColor.external))
                put("showOriginBadges", config.getBoolean(ConfigKeys.showOriginBadges))
                put("showUserBadges", config.getBoolean(ConfigKeys.showUserBadges))
                put("zoomPercent", config.getInt(ConfigKeys.zoomPercent))
                put("hideMessages", config.getBoolean(ConfigKeys.hideMessages))
                put("hideMessagesAfter", config.getInt(ConfigKeys.hideMessagesAfter))
                putObject("enabledOrigins").apply {
                    COUNTABLE_ORIGINS.forEach { origin ->
                        put(origin.commonName, config.getBoolean("${origin.commonName}.enabled"))
                    }
                }
            }
        }

        wsFrameSender.sendToAll(messageNode.toString())
    }

    fun sendClearChat() {
        val clearChatNode = nodeFactory.objectNode().apply {
            put("type", "clear-chat")
            putObject("content")
        }

        wsFrameSender.sendToAll(clearChatNode.toString())
    }

    fun send(jsonMessage: JsonNode) {
        wsFrameSender.sendToAll(jsonMessage.toString())
    }

}
