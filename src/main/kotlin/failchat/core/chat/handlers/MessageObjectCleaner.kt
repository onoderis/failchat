package failchat.core.chat.handlers

import failchat.core.chat.ChatMessage
import failchat.core.chat.MessageHandler

/**
 * Заменяет символы '{' и '}' на html entity.
 */
class MessageObjectCleaner<in T : ChatMessage> : MessageHandler<T> {

    override fun handleMessage(message: T) {
        message.text = message.text.apply {
            replace("{", "&#123;")
            replace("}", "&#125;")
        }
    }

}