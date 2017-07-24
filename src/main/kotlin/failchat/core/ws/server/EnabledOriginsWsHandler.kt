package failchat.core.ws.server

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.viewers.countableOrigins
import org.apache.commons.configuration.CompositeConfiguration

class EnabledOriginsWsHandler(
        private val config: CompositeConfiguration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        val enabledOriginsArray = countableOrigins
                .filter { config.getBoolean("${it.name}.enabled") }
                .fold(objectMapper.createArrayNode()) { acc, origin -> acc.add(origin.name) }

        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "enabled-origins")
            putObject("content").apply {
                set("origins", enabledOriginsArray)
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}
