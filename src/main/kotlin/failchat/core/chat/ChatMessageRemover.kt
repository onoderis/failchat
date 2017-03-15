package failchat.core.chat

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.ws.server.WsServer

class ChatMessageRemover(
        private val wsServer: WsServer,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    fun remove(messageId: Long) {
        val removeMessage = objectMapper.createObjectNode().apply {
            put("type", "mod")
            putObject("content").apply {
                put("messageId", messageId)
            }
        }
        wsServer.sendToAll(removeMessage.toString())
    }

    fun remove(message: ChatMessage) = remove(message.id)

}