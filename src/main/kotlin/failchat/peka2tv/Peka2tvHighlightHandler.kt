package failchat.peka2tv

import failchat.chat.MessageHandler

class Peka2tvHighlightHandler(private val channelName: String) : MessageHandler<Peka2tvMessage> {

    override fun handleMessage(message: Peka2tvMessage) {
        message.toUser?.let { appealedUser ->
            if (message.text.isEmpty()) return

            if (appealedUser.name.equals(channelName, ignoreCase = true)) message.highlighted = true
            message.text = "${appealedUser.name}, ${message.text}"

        }
    }

}
