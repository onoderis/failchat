package failchat.ws.server

import failchat.ConfigKeys
import failchat.chat.handlers.IgnoreFilter
import org.apache.commons.configuration2.Configuration

class IgnoreWsMessageHandler(
        private val ignoreFilter: IgnoreFilter,
        private val config: Configuration
) : WsMessageHandler {

    override val expectedType = InboundWsMessage.Type.IGNORE_AUTHOR

    override fun handle(message: InboundWsMessage) {
        val authorId = message.content.get("authorId").asText()
        val updatedIgnoreSet = config.getStringArray(ConfigKeys.ignore)
                .toMutableSet()
                .apply { add(authorId) }
        config.setProperty(ConfigKeys.ignore, updatedIgnoreSet)

        ignoreFilter.reloadConfig()
    }

}
