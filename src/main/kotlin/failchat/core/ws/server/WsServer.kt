package failchat.core.ws.server

interface WsServer {

    fun start()
    fun stop()

    fun sendToAll(message: String)
    fun sendToNativeClient(message: String)

    fun setOnMessage(type: String, consumer: WsMessageHandler)
    /**
     * Remove message handler.
     * @return previous callback for this type or null.
     * */
    fun removeOnMessage(type: String): WsMessageHandler?

}
