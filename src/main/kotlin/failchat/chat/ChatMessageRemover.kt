package failchat.chat

import com.fasterxml.jackson.databind.node.JsonNodeFactory

class ChatMessageRemover(
        private val chatMessageSender: ChatMessageSender
) {

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance

    fun remove(messageId: Long) {
        val removeMessage = nodeFactory.objectNode().apply {
            put("type", "delete-message")
            putObject("content").apply {
                put("messageId", messageId)
            }
        }
        chatMessageSender.send(removeMessage)
    }

    fun remove(message: ChatMessage) = remove(message.id)

}
