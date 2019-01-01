package failchat.chat

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import failchat.ConfigKeys
import failchat.Origin
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import failchat.emoticon.Emoticon
import failchat.util.nodeFactory
import failchat.util.objectMapper
import failchat.viewers.COUNTABLE_ORIGINS
import failchat.ws.server.WsFrameSender
import mu.KLogging
import org.apache.commons.configuration2.Configuration

class ChatMessageSender(
        private val wsFrameSender: WsFrameSender,
        private val config: Configuration
) {

    private companion object : KLogging()

    fun send(message: ChatMessage) {
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

    /** Send client configuration to all clients. */
    fun sendClientConfiguration() {
        val messageNode = nodeFactory.objectNode().apply {
            put("type", "client-configuration")
            putObject("content").apply {
                put("showViewersCount", config.getBoolean(ConfigKeys.showViewers))
                put("showOriginBadges", config.getBoolean(ConfigKeys.showOriginBadges))
                put("showUserBadges", config.getBoolean(ConfigKeys.showUserBadges))
                put("zoomPercent", config.getInt(ConfigKeys.zoomPercent))
                put("deletedMessagePlaceholder", config.getString(ConfigKeys.deletedMessagePlaceholder))
                put("hideDeletedMessages", config.getString(ConfigKeys.hideDeletedMessages))
                put("showHiddenMessages", config.getBoolean(ConfigKeys.showHiddenMessages))
                putObject("nativeClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.nativeClient.backgroundColor))
                    put("hideMessages", config.getBoolean(ConfigKeys.nativeClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.nativeClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.nativeClient.showStatusMessages))
                }
                putObject("externalClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.externalClient.backgroundColor))
                    put("hideMessages", config.getBoolean(ConfigKeys.externalClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.externalClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.externalClient.showStatusMessages))
                }
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

    fun sendConnectedOriginsMessage(statuses: Map<Origin, OriginStatus>) {
        val message = objectMapper.createObjectNode().apply {
            put("type", "origins-status")
            putObject("content").apply {
                statuses.forEach { (origin, status) ->
                    put(origin.commonName, status.jsonValue)
                }
            }
        }

        wsFrameSender.sendToAll(message.toString())
    }

    fun send(jsonMessage: JsonNode) {
        wsFrameSender.sendToAll(jsonMessage.toString())
    }

}
