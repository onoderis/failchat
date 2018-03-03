package failchat.chat

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.ws.server.WsServer

class ChatMessageRemover(
        private val wsServer: WsServer
) {

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance

    fun remove(messageId: Long) {
        val removeMessage = nodeFactory.objectNode().apply {
            put("type", "delete-message")
            putObject("content").apply {
                put("messageId", messageId)
            }
        }
        wsServer.send(removeMessage.toString())
    }

    fun remove(message: ChatMessage) = remove(message.id)

}
