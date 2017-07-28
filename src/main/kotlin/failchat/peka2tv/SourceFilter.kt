package failchat.peka2tv

import failchat.core.chat.MessageFilter

/**
 * Фильтруент сообщения с других источников
 */
class SourceFilter : MessageFilter<Peka2tvMessage> { //todo rename?
    override fun filterMessage(message: Peka2tvMessage): Boolean {
        return message.fromUser.id < 0
    }
}
