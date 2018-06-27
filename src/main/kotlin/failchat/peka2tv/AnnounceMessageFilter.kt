package failchat.peka2tv

import failchat.chat.MessageFilter
import mu.KLogging

class AnnounceMessageFilter : MessageFilter<Peka2tvMessage> {

    private companion object : KLogging()

    override fun filterMessage(message: Peka2tvMessage): Boolean {
        if (message.type == "announce") {
            logger.debug("Announce message filtered: {}", message.text)
            return true
        } else {
            return false
        }
    }

}
