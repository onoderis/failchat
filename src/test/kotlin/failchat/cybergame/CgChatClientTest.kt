package failchat.cybergame

import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatMessageHistory
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus
import failchat.chat.StatusUpdate
import failchat.config
import failchat.doAwhile
import failchat.ms
import failchat.privateConfig
import failchat.util.value
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertTrue

class CgChatClientTest {

    private val history = ChatMessageHistory(10)
    private lateinit var chatClient: CgChatClient

    private fun initChatClient(name: String, channelId: Long, callbacks: ChatClientCallbacks) {
        history.start()
        chatClient = CgChatClient(
                name,
                channelId,
                config.getString("cybergame.ws-url"),
                config.getString("cybergame.emoticon-url-prefix"),
                MessageIdGenerator(0),
                history,
                callbacks
        )
    }

    @After
    fun stopChatClient() {
        chatClient.stop()
        history.stop()
    }

    @Ignore
    @Test
    fun connectionTest() {
        val connected = AtomicBoolean(false)
        val onStatusMessage = { statusUpdate: StatusUpdate ->
            if (statusUpdate.status == OriginStatus.CONNECTED) {
                connected.set(true)
            }
        }
        val callbacks = ChatClientCallbacks({}, onStatusMessage, {})
        initChatClient("scatman", 10946, callbacks)

        chatClient.start()

        doAwhile(10, 500.ms()) {
            assertTrue(connected.value, "Chat client not connected")
        }
    }

    @Ignore
    @Test
    fun manualTest() {
        val callbacks = ChatClientCallbacks(
                { println("Status message:  $it") },
                { println("Message:         $it") },
                { println("Message deleted: $it") }
        )
        initChatClient(
                privateConfig.getString("test.cybergame.channel-name"),
                privateConfig.getLong("test.cybergame.channel-id"),
                callbacks
        )

        chatClient.start()

        readLine()
    }

}
