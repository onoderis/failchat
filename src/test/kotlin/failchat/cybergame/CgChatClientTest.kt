package failchat.cybergame

import failchat.chat.ChatMessageHistory
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus
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

    private fun initChatClient(name: String, channelId: Long) {
        history.start()
        chatClient = CgChatClient(
                name,
                channelId,
                config.getString("cybergame.ws-url"),
                config.getString("cybergame.emoticon-url-prefix"),
                MessageIdGenerator(0),
                history
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
        initChatClient("scatman", 10946)
        val connected = AtomicBoolean(false)
        chatClient.onStatusMessage = {
            if (it.status == OriginStatus.CONNECTED) {
                connected.set(true)
            }
        }

        chatClient.start()

        doAwhile(10, 500.ms()) {
            assertTrue(connected.value, "Chat client not connected")
        }
    }

    @Ignore
    @Test
    fun manualTest() {
        initChatClient(privateConfig.getString("test.cybergame.channel-name"),
                privateConfig.getLong("test.cybergame.channel-id"))
        chatClient.onStatusMessage = { println("Status message:  $it") }
        chatClient.onChatMessage = { println("Message:         $it") }
        chatClient.onChatMessageDeleted = { println("Message deleted: $it") }

        chatClient.start()

        readLine()
    }

}
