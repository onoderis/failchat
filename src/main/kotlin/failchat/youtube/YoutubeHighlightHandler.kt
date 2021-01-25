package failchat.youtube

import failchat.chat.MessageHandler
import failchat.util.LateinitVal

class YoutubeHighlightHandler : MessageHandler<YoutubeMessage> {

    private val appealedChannelTitle = LateinitVal<String>()

    override fun handleMessage(message: YoutubeMessage) {
        appealedChannelTitle.get()?.let {
            if (message.text.contains(it)) {
                message.highlighted = true
            }
        }
    }

    fun setChannelTitle(channelTitle: String) {
        appealedChannelTitle.set("@$channelTitle")
    }

}
