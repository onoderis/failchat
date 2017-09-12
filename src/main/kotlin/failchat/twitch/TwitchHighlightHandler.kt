package failchat.twitch

import failchat.chat.MessageHandler

class TwitchHighlightHandler(channel: String) : MessageHandler<TwitchMessage> {

    private val appeal = "@" + channel

    override fun handleMessage(message: TwitchMessage) {
        if (message.text.contains(appeal, ignoreCase = true)) {
            message.highlighted = true
        }
    }
}
