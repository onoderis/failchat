package failchat.ws.server

interface WsMessageHandler {
    fun handle(message: InboundWsMessage)
}
