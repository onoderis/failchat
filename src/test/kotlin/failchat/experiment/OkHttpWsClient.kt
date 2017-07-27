package failchat.experiment

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.util.sleep
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Ignore
import org.junit.Test
import java.time.Duration

@Ignore
class OkHttpWsClient {

    @Test
    fun tryIt() {
        val client = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build()

        val reuqest = Request.Builder()
                .url("ws://chat.goodgame.ru:8081/chat/websocket")
                .build()

        client.newWebSocket(reuqest, Listener())

        sleep(Duration.ofDays(1))
    }

    private class Listener : WebSocketListener() {
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

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) = response.use { println("onFailure $t") }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) = println("onClosing $code, $reason")

        override fun onMessage(webSocket: WebSocket?, text: String?) = println("onMessage: $text")

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) = println("onMessage bytes")

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) = println("onClosed $code $reason")
    }

}
/*
{"type":"channel_counters","data":{"channel_id":"20296","clients_in_channel":"3","users_in_channel":1}}
* */
