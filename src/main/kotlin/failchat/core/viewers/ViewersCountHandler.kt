package failchat.core.viewers

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.Origin.goodgame
import failchat.core.Origin.peka2tv
import failchat.core.Origin.twitch
import failchat.core.ws.server.InboundWsMessage
import failchat.core.ws.server.WsMessageHandler
import org.apache.commons.configuration.CompositeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ViewersCountHandler(
        private val config: CompositeConfiguration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) : WsMessageHandler {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCountHandler::class.java)
    }

    private val countableOrigins = listOf(peka2tv, goodgame, twitch)

    var viewersCounter: ViewersCounter? = null //todo volatile?

    override fun invoke(message: InboundWsMessage) {
        viewersCounter?.apply {
            sendViewersCountWsMessage()
            return
        }

        // Send message with null values of enabled origins
        val enabledOrigins = countableOrigins.filter {
            config.getBoolean("${it.name}.enabled")
        }

        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "viewers")
            putObject("content").apply {
                put("show", config.getBoolean("show-viewers"))
                enabledOrigins.forEach { putNull(it.name) }
            }
        }

        message.clientSocket.send(messageNode.toString())
    }

}
