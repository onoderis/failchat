package failchat.chat.handlers

import failchat.chat.ChatMessageSender
import failchat.chat.OriginStatusManager
import failchat.ws.server.InboundWsMessage
import failchat.ws.server.InboundWsMessage.Type.ORIGINS_STATUS
import failchat.ws.server.WsMessageHandler

class OriginsStatusHandler(
        private val originStatusManager: OriginStatusManager,
        private val messageSender: ChatMessageSender
) : WsMessageHandler {

    override val expectedType = ORIGINS_STATUS

    override fun handle(message: InboundWsMessage) {
        messageSender.sendConnectedOriginsMessage(originStatusManager.getStatuses())
    }
}
