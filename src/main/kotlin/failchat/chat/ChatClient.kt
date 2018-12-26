package failchat.chat

import failchat.Origin

/** Base interface for a chat client. Implementation should not be reusable. */
interface ChatClient {

    val origin: Origin
    val status: ChatClientStatus

    val callbacks: ChatClientCallbacks

    fun start()
    fun stop()

}
