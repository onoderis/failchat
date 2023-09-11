package failchat.peka2tv

import failchat.chat.MessageFilter
import mu.KotlinLogging

class AnnounceMessageFilter : MessageFilter<Peka2tvMessage> {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun filterMessage(message: Peka2tvMessage): Boolean {
        if (message.type == "announce") {
            logger.debug("Announce message filtered: {}", message.text)
            return true
        } else {
            return false
        }
    }

}
