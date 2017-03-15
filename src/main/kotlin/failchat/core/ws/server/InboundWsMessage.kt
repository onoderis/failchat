package failchat.core.ws.server

import com.fasterxml.jackson.databind.JsonNode
import org.java_websocket.WebSocket

class InboundWsMessage(
        val clientSocket: WebSocket,
        val content: JsonNode
)