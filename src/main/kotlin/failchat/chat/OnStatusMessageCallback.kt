package failchat.chat

import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.handlers.ConnectedOriginsHandler

class OnStatusMessageCallback(
        private val connectedOriginsHandler: ConnectedOriginsHandler,
        private val messageSender: ChatMessageSender
) : (StatusMessage) -> Unit {

    override fun invoke(message: StatusMessage) {
        when (message.status) {
            CONNECTED -> connectedOriginsHandler.addConnected(message.origin)
            DISCONNECTED -> connectedOriginsHandler.removeConnected(message.origin)
        }

        messageSender.send(message)
    }
}
