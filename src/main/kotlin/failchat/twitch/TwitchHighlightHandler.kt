package failchat.twitch

import failchat.core.chat.MessageHandler
import org.apache.commons.lang.StringUtils

class TwitchHighlightHandler(channel: String) : MessageHandler<TwitchMessage> {

    private val appeal = "@" + channel

    override fun handleMessage(message: TwitchMessage) {
        if (StringUtils.containsIgnoreCase(message.text, appeal)) {
            message.highlighted = true
        }
    }
}
