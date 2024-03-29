package failchat.chat

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import failchat.ConfigKeys
import failchat.Origin
import failchat.chat.badge.Badge
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import failchat.emoticon.Emoticon
import failchat.util.Do
import failchat.util.toHexFormat
import failchat.viewers.COUNTABLE_ORIGINS
import failchat.ws.server.WsFrameSender
import mu.KotlinLogging

//todo use dto
class ChatMessageSender(
        private val wsFrameSender: WsFrameSender,
        private val appConfiguration: AppConfiguration,
        private val objectMapper: ObjectMapper
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val config = appConfiguration.config

    fun send(message: ChatMessage) {
        val messageNode: ObjectNode = objectMapper.nodeFactory.objectNode().apply {
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
                put("highlightedBackground", message.highlightedBackground)

                set<ObjectNode>("elements", serializeElements(message.elements))

                set<ObjectNode>("badges", serializeBadges(message.badges))
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
                                .put("origin", element.origin.commonName)
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

        val messageNode = objectMapper.nodeFactory.objectNode().apply {
            put("type", "client-configuration")
            putObject("content").apply {
                put("showViewersCount", config.getBoolean(ConfigKeys.showViewers))
                put("showOriginBadges", config.getBoolean(ConfigKeys.showOriginBadges))
                put("showUserBadges", config.getBoolean(ConfigKeys.showUserBadges))
                put("zoomPercent", config.getInt(ConfigKeys.zoomPercent))
                put("deletedMessagePlaceholder", config.getString(ConfigKeys.deletedMessagePlaceholder))
                put("hideDeletedMessages", config.getBoolean(ConfigKeys.hideDeletedMessages))
                put("showHiddenMessages", config.getBoolean(ConfigKeys.showHiddenMessages))
                put("clickTransparency", config.getBoolean(ConfigKeys.clickTransparency))
                put("showClickTransparencyIcon", config.getBoolean(ConfigKeys.showClickTransparencyIcon))
                putObject("nativeClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.NativeClient.backgroundColor))
                    put("coloredNicknames", config.getBoolean(ConfigKeys.NativeClient.coloredNicknames))
                    put("hideMessages", config.getBoolean(ConfigKeys.NativeClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.NativeClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.NativeClient.showStatusMessages))
                }
                putObject("externalClient").apply {
                    put("backgroundColor", config.getString(ConfigKeys.ExternalClient.backgroundColor))
                    put("coloredNicknames", config.getBoolean(ConfigKeys.ExternalClient.coloredNicknames))
                    put("hideMessages", config.getBoolean(ConfigKeys.ExternalClient.hideMessages))
                    put("hideMessagesAfter", config.getInt(ConfigKeys.ExternalClient.hideMessagesAfter))
                    put("showStatusMessages", config.getBoolean(ConfigKeys.ExternalClient.showStatusMessages))
                }
                putObject("enabledOrigins").apply {
                    COUNTABLE_ORIGINS.forEach { origin ->
                        put(origin.commonName, config.getBoolean("${origin.commonName}.enabled"))
                    }
                }
                putObject("deletedMessagePlaceholder").apply {
                    put("text", deletedMessagePlaceholder.text)
                    set<ObjectNode>("elements", serializeElements(deletedMessagePlaceholder.emoticons))
                }
            }
        }

        wsFrameSender.sendToAll(messageNode.toString())
    }

    fun sendClearChat() {
        val clearChatNode = objectMapper.nodeFactory.objectNode().apply {
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
