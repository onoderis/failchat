package failchat.core.ws.server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * WsServer implementation for TooTallNate's java websocket library.
 **/
class TtnWsServer(private val om: ObjectMapper = ObjectMapper()) : WsServer {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TtnWsServer::class.java)
        val defaultAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 10880)
    }

    private val wsServerImpl = WsServerImpl(defaultAddress)
    private val messageConsumers: MutableMap<String, WsMessageHandler> = HashMap()

    // Native client - браузер, запущенный в приложении
    private var nativeClient: WebSocket? = null


    override fun start() = wsServerImpl.start()

    override fun stop() = wsServerImpl.stop()

    override fun sendToAll(message: String) {

        val connections = wsServerImpl.connections()
        /*
        * Нужна синхронизация по коллекции с WebSocket'ами т.к. библеотека отдаёт не копию, и объект с которым она работает.
        * Сама библиотека так же использует блок synchronized.
        * */
        synchronized(connections) {
            connections.forEach { it.send(message) }
        }

        log.debug("Sent to all web socket clients: {}", message)
    }

    override fun sendToNativeClient(message: String) {
        nativeClient?.let { webSocket ->
            if (webSocket.isOpen) {
                webSocket.send(message)
                log.debug("Sent to native web socket client: {}", message)
            }
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
            log.info("Web socket connection opened. client address: '{}'", webSocket.remoteSocketAddress)
        }

        override fun onClose(webSocket: WebSocket, i: Int, s: String, b: Boolean) {
            log.info("Web socket connection closed. client address: '{}'", webSocket.remoteSocketAddress)
        }

        override fun onMessage(webSocket: WebSocket, inboundMessage: String) {
            log.debug("Received message from web socket client. address: {}, message: {}", webSocket.remoteSocketAddress, inboundMessage)

            val parsedMessage: JsonNode = om.readTree(inboundMessage)
            val type: String = parsedMessage.get("type").asText()

            messageConsumers[type]
                    ?.invoke(InboundWsMessage(webSocket, parsedMessage.get("content")))
                    ?: log.warn("Message consumer not found for message with type '{}'. Message: '{}'", type, inboundMessage)
        }

        override fun onError(webSocket: WebSocket, e: Exception) {
            log.warn("Web socket error. client address: '{}'", webSocket.remoteSocketAddress, e)
        }

    }

}
