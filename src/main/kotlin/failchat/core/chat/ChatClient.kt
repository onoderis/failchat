package failchat.core.chat

import failchat.core.Origin

/**
* Not reusable.
* */
interface ChatClient<out T : ChatMessage> {

    fun start()
    fun stop()

    val origin: Origin
    val status: ChatClientStatus

    fun onChatMessage(consumer: (T) -> Unit)

    fun onStatusMessage(consumer: (StatusMessage) -> Unit)

    fun onChatMessageDeleted(operation: (T) -> Unit)

}
