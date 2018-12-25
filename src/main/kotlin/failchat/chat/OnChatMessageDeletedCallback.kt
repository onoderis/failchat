package failchat.chat

class OnChatMessageDeletedCallback(
        private val chatMessageRemover: ChatMessageRemover
) : (ChatMessage) -> Unit {

    override fun invoke(message: ChatMessage) {
        chatMessageRemover.remove(message)
    }
}
