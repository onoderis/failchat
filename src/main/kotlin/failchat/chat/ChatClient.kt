package failchat.chat

import failchat.Origin

/** Base interface for a chat client. Implementation should not be reusable. */
interface ChatClient<T : ChatMessage> {

    val origin: Origin
    val status: ChatClientStatus

    fun start()
    fun stop()

}
