package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.Elements
import failchat.chat.MessageHandler

/**
 * Заменяет символы '{' и '}' на html entity.
 */
class ElementLabelEscaper<in T : ChatMessage> : MessageHandler<T> {

    override fun handleMessage(message: T) {
        message.text = Elements.escapeLabelCharacters(message.text)
    }

}
