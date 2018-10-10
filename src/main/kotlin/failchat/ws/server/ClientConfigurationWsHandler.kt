package failchat.ws.server

import failchat.chat.ChatMessageSender

class ClientConfigurationWsHandler(
        private val messageSender: ChatMessageSender
) : WsMessageHandler {

    override val expectedType = InboundWsMessage.Type.CLIENT_CONFIGURATION

    override fun handle(message: InboundWsMessage) {
        messageSender.sendClientConfiguration()
    }

}
