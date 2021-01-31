package failchat.twitch

import failchat.chat.MessageHandler

class TwitchRewardHandler : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        if (message.tags.get(TwitchIrcTags.customRewardId) != null) {
            message.highlightedBackground = true
        }
    }

}
