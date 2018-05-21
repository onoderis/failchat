package failchat.peka2tv

import failchat.chat.MessageFilter

/**
 * Фильтруент сообщения с других источников.
 */
class Peka2tvOriginFilter : MessageFilter<Peka2tvMessage> {
    override fun filterMessage(message: Peka2tvMessage): Boolean {
        return message.fromUser.id < 0
    }
}
