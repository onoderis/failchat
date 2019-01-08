package failchat.chat

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import failchat.ConfigKeys
import failchat.Origin
import failchat.chat.badge.Badge
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import failchat.emoticon.Emoticon
import failchat.util.Do
import failchat.util.nodeFactory
import failchat.util.objectMapper
import failchat.util.toHexFormat
import failchat.viewers.COUNTABLE_ORIGINS
import failchat.ws.server.WsFrameSender
import mu.KLogging

class ChatMessageSender(
        private val wsFrameSender: WsFrameSender,
        private val appConfiguration: AppConfiguration
) {

    private companion object : KLogging()

    private val config = appConfiguration.config

    fun send(message: ChatMessage) {
        val messageNode = nodeFactory.objectNode().apply {
            put("type", "message")
            with("content").apply {
                put("id", message.id)
                put("origin", message.origin.commonName)
                putObject("author").apply {
                    put("name", message.author.name)
                    put("id", message.author.id)
                    put("color", message.author.color?.toHexFormat())
                }
                put("text", message.text)
                put("timestamp", message.timestamp.toEpochMilli())
                put("highlighted", message.highlighted)

                set("elements", serializeElements(message.elements))

                set("badges", serializeBadges(message.badges))
            }
        }

        wsFrameSender.sendToAll(messageNode.toString())
    }

    private fun serializeElements(elements: List<MessageElement>): ArrayNode {
        val elementsNode = objectMapper.createArrayNode()

        elements.asSequence()
                .map { element ->
                    when (element) {
                        is Emoticon -> objectMapper.createObjectNode()
                                .put("type", "emoticon") //todo enum
                                .put("code", element.code)
                                .put("url", element.url)
                                .put("format", element.format.jsonValue)

                        is Link -> objectMapper.createObjectNode()
                                .put("type", "link")
                                .put("domain", element.domain)
                                .put("fullUrl", element.fullUrl)
                                .put("shortUrl", element.shortUrl)

                        is Image -> objectMapper.createObjectNode()
                                .put("type", "image")
                                .put("url", element.link.fullUrl)

                        else -> {
                            logger.error("Unknown element type: {}", element.javaClass.name)
                            null
                        }
                    }
                }
                .filterNotNull()
                .forEach {
                    elementsNode.add(it)
                }

        return elementsNode
    }

    private fun serializeBadges(badges: List<Badge>): ArrayNode {
        val badgesNode = objectMapper.createArrayNode()

        badges.forEach { badge ->
            badgesNode.addObject().apply {
                Do exhaustive when (badge) {
                    is ImageBadge -> {
                        put("type", "image")
                        put("format", badge.format.jsonValue)
                        put("url", badge.url)
                        put("description", badge.description)
                    }
                    is CharacterBadge -> {
                        put("type", "character")
                        put("htmlEntity", badge.characterEntity)
                        put("color", badge.color.toHexFormat())
                    }
                }
            }
        }

        return badgesNode
    }

    /** Send client configuration to all clients. */
    fun sendClientConfiguration() {
        val deletedMessagePlaceholder = appConfiguration.deletedMessagePlaceholder

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "client-configuration")
            putObject("content").apply {
                put("showViewersCount", config.getBoolean(ConfigKeys.showViewers))
                put("showOriginBadges", config.getBoolean(ConfigKeys.showOriginBadges))
                put("showUserBadges", config.getBoolean(ConfigKeys.showUserBadges))
                put("zoomPercent", config.getInt(ConfigKeys.zoomPercent))
                put("deletedMessagePlaceholder", config.getString(ConfigKeys.deletedMessagePlaceholder))
                put("hideDeletedMessages", config.getBoolean(ConfigKeys.hideDeletedMessages))
                put("showHiddenMessages", config.getBoolean(ConfigKeys.showHiddenMessages))
                putObject("nativeClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.nativeClient.backgroundColor))
                    put("coloredNicknames", config.getBoolean(ConfigKeys.nativeClient.coloredNicknames))
                    put("hideMessages", config.getBoolean(ConfigKeys.nativeClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.nativeClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.nativeClient.showStatusMessages))
                }
                putObject("externalClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.externalClient.backgroundColor))
                    put("coloredNicknames", config.getBoolean(ConfigKeys.externalClient.coloredNicknames))
                    put("hideMessages", config.getBoolean(ConfigKeys.externalClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.externalClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.externalClient.showStatusMessages))
                }
                putObject("enabledOrigins").apply {
                    COUNTABLE_ORIGINS.forEach { origin ->
                        put(origin.commonName, config.getBoolean("${origin.commonName}.enabled"))
                    }
                }
                putObject("deletedMessagePlaceholder").apply {
                    put("text", deletedMessagePlaceholder.text)
                    set("elements", serializeElements(deletedMessagePlaceholder.emoticons))
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
