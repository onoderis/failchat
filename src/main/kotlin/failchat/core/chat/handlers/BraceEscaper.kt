package failchat.core.chat.handlers

import failchat.core.chat.ChatMessage
import failchat.core.chat.MessageHandler

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
