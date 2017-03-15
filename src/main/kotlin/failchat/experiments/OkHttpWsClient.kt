package failchat.experiments

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

fun main(args: Array<String>) {
    val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()

    val reuqest = Request.Builder()
            .url("ws://chat.goodgame.ru:8081/chat/websocket")
            .build()

    client.newWebSocket(reuqest, Listner())
}

class Listner : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("onOpen")

        val joinMessage = ObjectMapper().createObjectNode().apply {
            put("type", "join")
            putObject("data").apply {
                put("channel_id", 20296)
                put("isHidden", false)
            }
        }

        webSocket.send(joinMessage.toString())
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) = println("onFailure $t")

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) = println("onClosing $code, $reason")

    override fun onMessage(webSocket: WebSocket?, text: String?) = println("onMessage: $text")

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) = println("onMessage bytes")

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) = println("onClosed $code $reason")
}
