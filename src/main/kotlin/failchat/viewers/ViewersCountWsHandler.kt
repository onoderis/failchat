package failchat.viewers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.ws.server.InboundWsMessage
import failchat.ws.server.WsMessageHandler
import io.ktor.http.cio.websocket.Frame
import org.apache.commons.configuration2.Configuration


class ViewersCountWsHandler(
        private val config: Configuration
) : WsMessageHandler {

    override val expectedType = InboundWsMessage.Type.VIEWERS_COUNT

    @Volatile
    var viewersCounter: ViewersCounter? = null

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance

    
    override fun handle(message: InboundWsMessage) {
        viewersCounter?.let {
            it.sendViewersCountWsMessage()
            return
        }

        // viewersCounter is not set yet
        // send message with null values for enabled origins
        val enabledOrigins = COUNTABLE_ORIGINS.filter {
            config.getBoolean("${it.commonName}.enabled")
        }

        val messageNode = nodeFactory.objectNode().apply {
            put("type", "viewers-count")
            putObject("content").apply {
                enabledOrigins.forEach { putNull(it.commonName) }
            }
        }

        message.session.outgoing.offer(Frame.Text(messageNode.toString()))
    }

}
