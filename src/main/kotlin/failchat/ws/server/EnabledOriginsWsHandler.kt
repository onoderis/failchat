package failchat.ws.server

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.viewers.countableOrigins
import org.apache.commons.configuration2.Configuration

class EnabledOriginsWsHandler(
        private val config: Configuration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val messageNode = objectMapper.createObjectNode().apply {
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
