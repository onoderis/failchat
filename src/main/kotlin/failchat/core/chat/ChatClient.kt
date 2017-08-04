package failchat.core.chat

import failchat.core.Origin

/**
* Not reusable.
* */
interface ChatClient<T : ChatMessage> {

    val origin: Origin
    val status: ChatClientStatus

    var onChatMessage: ((T) -> Unit)?
    var onStatusMessage: ((StatusMessage) -> Unit)?
    var onChatMessageDeleted: ((T) -> Unit)?

    fun start()
    fun stop()

}
