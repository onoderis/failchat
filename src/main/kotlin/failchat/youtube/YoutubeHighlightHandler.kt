package failchat.youtube

import failchat.chat.MessageHandler
import java.util.concurrent.atomic.AtomicReference

class YoutubeHighlightHandler : MessageHandler<YoutubeMessage> {

    private val appealedChannelTitle = AtomicReference<String?>()

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
