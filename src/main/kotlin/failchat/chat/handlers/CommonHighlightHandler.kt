package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.MessageHandler

class CommonHighlightHandler<in T>(username: String) : MessageHandler<T>
where T : ChatMessage {

    private val appeal: String = username + ','

    override fun handleMessage(message: T) {
        if (message.text.contains(appeal, ignoreCase = true)) {
            message.highlighted = true
        }
    }

}
