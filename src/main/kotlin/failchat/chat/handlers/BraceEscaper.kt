package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.Elements
import failchat.chat.MessageHandler

/**
 * Заменяет символы '<' и '>' на html character entities.
 */
class BraceEscaper : MessageHandler<ChatMessage> {
    override fun handleMessage(message: ChatMessage) {
        message.text = Elements.escapeBraces(message.text)
    }
}
