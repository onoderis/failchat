package failchat.core.viewers

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.ws.server.InboundWsMessage
import failchat.core.ws.server.WsMessageHandler
import org.apache.commons.configuration.CompositeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ViewersCountWsHandler(
        private val config: CompositeConfiguration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCountWsHandler::class.java)
    }

    var viewersCounter: ViewersCounter? = null //todo volatile?

    override fun invoke(message: InboundWsMessage) {
        viewersCounter?.apply {
            sendViewersCountWsMessage()
            return
        }

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
