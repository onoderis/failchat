package failchat.cybergame

import failchat.config
import failchat.okHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class ApiClientTest {

    private val apiClient = CgApiClient(okHttpClient, config.getString("cybergame.api-url"))

    @Test
    fun getChannelIdTest() {
        val channelName = "scatman"
        val channelId = runBlocking { apiClient.requestChannelId(channelName) }
        assertEquals(10946, channelId)
    }

}
