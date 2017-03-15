package failchat.core.ws.server

import failchat.core.chat.ChatMessageRemover
import failchat.core.chat.handlers.IgnoreFilter
import org.apache.commons.configuration.CompositeConfiguration

class IgnoreWsMessageHandler(
        private val ignoreFilter: IgnoreFilter,
        private val chatMessageRemover: ChatMessageRemover,
        private val config: CompositeConfiguration
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val username = message.content.get("user").asText()
        val updatedIgnoreSet = config.getStringArray("ignore")
                .toMutableSet()
                .apply { add(username) }
        config.setProperty("ignore", updatedIgnoreSet)

        ignoreFilter.reloadConfig()

        chatMessageRemover.remove(message.content.get("messageId").asLong())
    }

}