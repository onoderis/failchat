package failchat.ws.server

import failchat.chat.ChatMessageRemover

class DeleteWsMessageHandler(
        private val chatMessageRemover: ChatMessageRemover
) : WsMessageHandler {

    override fun handle(message: InboundWsMessage) {
        chatMessageRemover.remove(message.content.get("messageId").asLong())
    }

}
