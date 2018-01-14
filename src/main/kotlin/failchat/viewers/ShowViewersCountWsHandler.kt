package failchat.viewers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.ws.server.InboundWsMessage
import failchat.ws.server.WsMessageHandler
import org.apache.commons.configuration2.Configuration

class ShowViewersCountWsHandler(
        private val config: Configuration
) : WsMessageHandler {

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
    
    override fun invoke(message: InboundWsMessage) {
        val messageNode = nodeFactory.objectNode().apply {
            put("type", "show-viewers-count")
            putObject("content").apply {
                put("show", config.getBoolean("show-viewers"))
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}

