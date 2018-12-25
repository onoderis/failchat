package failchat.chat

import kotlinx.coroutines.runBlocking

class OnChatMessageCallback(
        private val filters: List<MessageFilter<ChatMessage>>,
        private val handlers: List<MessageHandler<ChatMessage>>,
        private val messageHistory: ChatMessageHistory,
        private val messageSender: ChatMessageSender
) : (ChatMessage) -> Unit {

    override fun invoke(message: ChatMessage) {
        // apply filters and handlers
        filters.forEach {
            if (it.filterMessage(message)) return
        }
        handlers.forEach { it.handleMessage(message) }

        runBlocking {
            messageHistory.add(message)
        }

        messageSender.send(message)
    }
}
