package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.MessageHandler

/**
 * Заменяет символы '<' и '>' на html character entities.
 */
class BraceEscaper : MessageHandler<ChatMessage> {
    override fun handleMessage(message: ChatMessage) {
        message.text = message.text
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}
