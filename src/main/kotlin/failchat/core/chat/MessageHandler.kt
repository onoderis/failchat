package failchat.core.chat

interface MessageHandler<in T : ChatMessage> {
    fun handleMessage(message: T)
}
