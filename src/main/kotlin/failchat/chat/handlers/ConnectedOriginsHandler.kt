package failchat.chat.handlers

import failchat.Origin
import failchat.chat.ChatMessageSender
import failchat.util.enumSet
import failchat.util.objectMapper
import failchat.ws.server.InboundWsMessage
import failchat.ws.server.InboundWsMessage.Type.CONNECTED_ORIGINS
import failchat.ws.server.WsMessageHandler
import java.util.Collections

class ConnectedOriginsHandler(private val messageSender: ChatMessageSender) : WsMessageHandler {

    private val connectedOrigins: MutableSet<Origin> = Collections.synchronizedSet(enumSet())

    override val expectedType = CONNECTED_ORIGINS

    override fun handle(message: InboundWsMessage) {
        val snapshot = connectedOrigins.toTypedArray() // Iterator is not synchronized
        sendConnectedOriginsMessage(snapshot)
    }

    fun addConnected(origin: Origin) {
        connectedOrigins.add(origin)
    }

    fun removeConnected(origin: Origin) {
        connectedOrigins.remove(origin)
    }

    fun reset() {
        connectedOrigins.clear()
    }

    private fun sendConnectedOriginsMessage(origins: Array<Origin>) {
        val responseMessage = objectMapper.createObjectNode().apply {
            put("type", "connected-origins")
            putObject("content").apply {
                putArray("origins").apply {
                    origins.forEach { add(it.commonName) }
                }
            }
        }

        messageSender.send(responseMessage)
    }
}
