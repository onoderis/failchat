package failchat.youtube

import failchat.core.chat.MessageHandler
import failchat.util.value
import java.util.concurrent.atomic.AtomicReference

class YtHighlightHandler: MessageHandler<YtMessage> {

    val channelTitle: AtomicReference<String?> = AtomicReference(null)

    override fun handleMessage(message: YtMessage) {
        channelTitle.value?.let {
            if (message.text.contains(it)) message.highlighted = true
        }
    }
}
