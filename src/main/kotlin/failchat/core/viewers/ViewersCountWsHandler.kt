package failchat.core.viewers

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.ws.server.InboundWsMessage
import failchat.core.ws.server.WsMessageHandler
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference


class ViewersCountWsHandler(
        private val config: Configuration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCountWsHandler::class.java)
    }

    val viewersCounter: AtomicReference<ViewersCounter?> = AtomicReference(null)

    override fun invoke(message: InboundWsMessage) {
        viewersCounter.get()?.let {
            it.sendViewersCountWsMessage()
            return
        }

        // viewersCounter is null
        // Send message with null values for enabled origins
        val enabledOrigins = countableOrigins.filter {
            config.getBoolean("${it.name}.enabled")
        }

        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "viewers-count")
            putObject("content").apply {
                enabledOrigins.forEach { putNull(it.name) }
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}
