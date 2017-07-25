package failchat.core.ws.server

import failchat.core.chat.handlers.IgnoreFilter
import org.apache.commons.configuration2.Configuration

class IgnoreWsMessageHandler(
        private val ignoreFilter: IgnoreFilter,
        private val config: Configuration
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val username = message.content.get("user").asText()
        val updatedIgnoreSet = config.getStringArray("ignore")
                .toMutableSet()
                .apply { add(username) }
        config.setProperty("ignore", updatedIgnoreSet)

        ignoreFilter.reloadConfig()
    }

}
