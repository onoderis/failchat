package failchat.chat

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.ws.server.WsServer

class ChatMessageRemover(
        private val wsServer: WsServer,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    fun remove(messageId: Long) {
        val removeMessage = objectMapper.createObjectNode().apply {
            put("type", "delete-message")
            putObject("content").apply {
                put("messageId", messageId)
            }
        }
        wsServer.send(removeMessage.toString())
    }

    fun remove(message: ChatMessage) = remove(message.id)

}
