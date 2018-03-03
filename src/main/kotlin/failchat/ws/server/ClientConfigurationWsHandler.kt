package failchat.ws.server

import failchat.chat.ChatMessageSender

class ClientConfigurationWsHandler(
        private val messageSender: ChatMessageSender
) : WsMessageHandler {

    override fun handle(message: InboundWsMessage) {
        messageSender.sendClientConfiguration()
    }

}
