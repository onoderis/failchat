package failchat.twitch

import failchat.chat.MessageHandler

class TwitchHighlightByPointsHandler : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        if (message.tags.get(TwitchIrcTags.msgId) == "highlighted-message") {
            message.highlightedBackground = true
        }
    }

}
