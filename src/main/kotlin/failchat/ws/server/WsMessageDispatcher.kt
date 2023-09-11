package failchat.ws.server

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.util.enumMap
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging

class WsMessageDispatcher(
        private val objectMapper: ObjectMapper,
        handlers: List<WsMessageHandler>
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val handlers: Map<InboundWsMessage.Type, WsMessageHandler> = handlers
            .map { it.expectedType to it }
            .toMap(enumMap<InboundWsMessage.Type, WsMessageHandler>())

    suspend fun handleWebSocket(session: DefaultWebSocketServerSession) {
        session.incoming.consumeEach { frame ->
            if (frame !is Frame.Text) {
                logger.warn("Non textual frame received: {}", frame)
                return@consumeEach
            }

            val frameText = frame.readText()
            logger.debug("Message received from a web socket client: {}", frameText)

            val parsedMessage = objectMapper.readTree(frameText)
            val typeString: String = parsedMessage.get("type").asText()
            val type = InboundWsMessage.Type.from(typeString)
                    ?: run {
                        logger.warn("Message received with unknown type '{}'", typeString)
                        return@consumeEach
                    }

            handlers[type]
                    ?.handle(InboundWsMessage(type, parsedMessage.get("content"), session))
                    ?: logger.warn("Message consumer not found for a message with a type '{}'. Message: {}", type, frameText)
        }
    }

}
