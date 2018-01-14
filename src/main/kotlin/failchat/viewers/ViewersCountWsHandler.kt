package failchat.viewers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.ws.server.InboundWsMessage
import failchat.ws.server.WsMessageHandler
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference


class ViewersCountWsHandler(
        private val config: Configuration
) : WsMessageHandler {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCountWsHandler::class.java)
    }

    val viewersCounter: AtomicReference<ViewersCounter?> = AtomicReference(null)
    
    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance

    
    override fun invoke(message: InboundWsMessage) {
        viewersCounter.get()?.let {
            it.sendViewersCountWsMessage()
            return
        }

        // viewersCounter is null
        // Send message with null values for enabled origins
        val enabledOrigins = countableOrigins.filter {
            config.getBoolean("${it.commonName}.enabled")
        }

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "viewers-count")
            putObject("content").apply {
                enabledOrigins.forEach { putNull(it.commonName) }
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}
