package failchat.chat

interface MessageFilter<in T : ChatMessage> {
    /**
     * @return true if message should be dropped
     * *
     */
    fun filterMessage(message: T): Boolean
}
