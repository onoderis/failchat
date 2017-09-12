package failchat.chat

interface MessageHandler<in T : ChatMessage> {
    fun handleMessage(message: T)
}
