package failchat.core.viewers

import failchat.core.ws.server.InboundWsMessage
import failchat.core.ws.server.WsMessageHandler

class ViewersCountHandler(private val viewersCounter: ViewersCounter) : WsMessageHandler {

    override fun invoke(message: InboundWsMessage) {
        viewersCounter.sendViewersCountWsMessage()
    }

}