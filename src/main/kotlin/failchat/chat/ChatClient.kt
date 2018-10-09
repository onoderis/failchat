package failchat.chat

import failchat.Origin

/** Base interface for a chat client. Implementation should not be reusable. */
interface ChatClient<T : ChatMessage> {

    val origin: Origin
    val status: ChatClientStatus

    var onChatMessage: ((T) -> Unit)?
    var onStatusMessage: ((StatusMessage) -> Unit)?
    var onChatMessageDeleted: ((T) -> Unit)?

    fun start()
    fun stop()

}
