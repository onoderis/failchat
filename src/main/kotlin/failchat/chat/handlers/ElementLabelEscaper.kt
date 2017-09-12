package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.MessageHandler

/**
 * Заменяет символы '{' и '}' на html entity.
 */
class ElementLabelEscaper<in T : ChatMessage> : MessageHandler<T> {

    override fun handleMessage(message: T) {
        message.text = message.text
                .replace("{", "&#123;")
                .replace("}", "&#125;")
    }

}
