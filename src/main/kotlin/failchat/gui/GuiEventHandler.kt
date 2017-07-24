package failchat.gui

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.ws.server.WsServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GuiEventHandler(
        private val wsServer: WsServer,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuiEventHandler::class.java)
    }

    fun fireViewersCountToggle(show: Boolean) {
        val messageNode = objectMapper.createObjectNode().apply {
            put("type", "show-viewers-count")
            putObject("content").apply {
                put("show", show)
            }
        }

        wsServer.sendToAll(messageNode.toString())
    }

}
