package failchat.ws.server

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.websocket.DefaultWebSocketServerSession

class InboundWsMessage(
        val type: Type,
        val content: JsonNode,
        val session: DefaultWebSocketServerSession
) {

    enum class Type(val jsonRepresentation: String) {
        CLIENT_CONFIGURATION("client-configuration"),
        DELETE_MESSAGE("delete-message"),
        IGNORE_AUTHOR("ignore-author"),
        VIEWERS_COUNT("viewers-count"),
        ORIGINS_STATUS("origins-status");

        companion object {
            private val map = values()
                    .map { it.jsonRepresentation to it }
                    .toMap()

            fun from(string: String): Type? = map[string]
        }
    }
}
