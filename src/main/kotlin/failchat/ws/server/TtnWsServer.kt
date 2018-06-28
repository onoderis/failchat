package failchat.ws.server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

/**
 * WsServer implementation for TooTallNate's java websocket library.
 **/
class TtnWsServer(
        private val address: InetSocketAddress,
        private val om: ObjectMapper = ObjectMapper()
) : WsServer {

    private companion object : KLogging()

    private val wsServerImpl = WsServerImpl(address)
    private val messageConsumers: MutableMap<String, WsMessageHandler> = HashMap()

    override fun start() {
        wsServerImpl.start()
        logger.info("Websocket server started at '{}'", address)
    }

    override fun stop() {
        wsServerImpl.stop()
        logger.info("Websocket server stopped at '{}'", address)
    }

    override fun send(message: String) {
        logger.debug("Sending to all web socket clients: {}", message)

        wsServerImpl.connections.forEach {
            it.send(message)
        }
    }

    override fun setOnMessage(type: String, consumer: WsMessageHandler) {
        messageConsumers[type] = consumer
    }

    override fun removeOnMessage(type: String): WsMessageHandler? {
        return messageConsumers.remove(type)
    }


    private inner class WsServerImpl(address: InetSocketAddress) : WebSocketServer(address) {

        override fun onStart() {
        }

        override fun onOpen(webSocket: WebSocket, clientHandshake: ClientHandshake) {
            logger.info("Web socket connection opened. client address: '{}'", webSocket.remoteSocketAddress)
        }

        override fun onClose(webSocket: WebSocket, i: Int, s: String, b: Boolean) {
            logger.info("Web socket connection closed. client address: '{}'", webSocket.remoteSocketAddress)
        }

        override fun onMessage(webSocket: WebSocket, inboundMessage: String) {
            logger.debug("Received message from web socket client. address: '{}', message: {}", webSocket.remoteSocketAddress, inboundMessage)

            val parsedMessage: JsonNode = om.readTree(inboundMessage)
            val type: String = parsedMessage.get("type").asText()

            messageConsumers[type]
                    ?.handle(InboundWsMessage(webSocket, parsedMessage.get("content")))
                    ?: logger.warn("Message consumer not found for message with type '{}'. Message: '{}'", type, inboundMessage)
        }

        override fun onError(webSocket: WebSocket, e: Exception) {
            logger.warn("Web socket error. client address: '{}'", webSocket.remoteSocketAddress, e)
        }

    }

}
