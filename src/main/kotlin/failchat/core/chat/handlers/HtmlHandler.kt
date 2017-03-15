package failchat.core.chat.handlers

import failchat.core.chat.ChatMessage
import failchat.core.chat.MessageHandler

/**
 * Заменяет символы '<' и '>' на html character entities.
 */
class HtmlHandler : MessageHandler<ChatMessage> {
    override fun handleMessage(message: ChatMessage) {
        message.text = message.text.apply {
            replace("<", "&lt;")
            replace(">", "&gt;")
        }
    }
}
