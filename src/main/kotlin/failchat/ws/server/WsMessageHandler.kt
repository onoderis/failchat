package failchat.ws.server

interface WsMessageHandler {
    val expectedType: InboundWsMessage.Type
    fun handle(message: InboundWsMessage)
}
