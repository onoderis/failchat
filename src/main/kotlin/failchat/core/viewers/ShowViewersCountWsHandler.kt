package failchat.core.viewers

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.ws.server.InboundWsMessage
import failchat.core.ws.server.WsMessageHandler
import org.apache.commons.configuration2.Configuration

class ShowViewersCountWsHandler(
        private val config: Configuration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "show-viewers-count")
            putObject("content").apply {
                put("show", config.getBoolean("show-viewers"))
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}

