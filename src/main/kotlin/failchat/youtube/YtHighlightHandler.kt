package failchat.youtube

import failchat.chat.MessageHandler
import failchat.util.LateinitVal

class YtHighlightHandler: MessageHandler<YtMessage> {

    val channelTitle = LateinitVal<String>()

    override fun handleMessage(message: YtMessage) {
        channelTitle.get()?.let {
            if (message.text.contains(it)) message.highlighted = true
        }
    }
}
