package failchat.ws.server

import failchat.chat.handlers.IgnoreFilter
import org.apache.commons.configuration2.Configuration

class IgnoreWsMessageHandler(
        private val ignoreFilter: IgnoreFilter,
        private val config: Configuration
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val authorId = message.content.get("authorId").asText()
        val updatedIgnoreSet = config.getStringArray("ignore")
                .toMutableSet()
                .apply { add(authorId) }
        config.setProperty("ignore", updatedIgnoreSet)

        ignoreFilter.reloadConfig()
    }

}
