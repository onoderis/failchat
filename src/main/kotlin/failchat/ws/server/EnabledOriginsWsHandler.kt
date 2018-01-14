package failchat.ws.server

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.viewers.countableOrigins
import org.apache.commons.configuration2.Configuration

class EnabledOriginsWsHandler(
        private val config: Configuration
) : WsMessageHandler {

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
    
    override fun invoke(message: InboundWsMessage) {
        val messageNode = nodeFactory.objectNode().apply {
            put("type", "enabled-origins")
            putObject("content").apply {
                countableOrigins.forEach { origin ->
                    put(origin.commonName, config.getBoolean("${origin.commonName}.enabled"))
                }
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}
