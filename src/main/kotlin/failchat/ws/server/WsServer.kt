package failchat.ws.server

interface WsServer {

    fun start()
    fun stop()

    fun send(message: String)

    /** Set message handler. */
    fun setOnMessage(type: String, consumer: WsMessageHandler)

    /**
     * Remove message handler.
     * @return previous callback for this type or null.
     * */
    fun removeOnMessage(type: String): WsMessageHandler?

}
