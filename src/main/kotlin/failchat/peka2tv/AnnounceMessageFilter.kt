package failchat.peka2tv

import failchat.core.chat.MessageFilter
import org.slf4j.LoggerFactory

class AnnounceMessageFilter : MessageFilter<Peka2tvMessage> {

    companion object {
        val logger: org.slf4j.Logger = LoggerFactory.getLogger(AnnounceMessageFilter::class.java)
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
