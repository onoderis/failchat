package failchat.twitch

import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class BttvApiClientTest {

    private val client = BttvApiClient(
        httpClient = okHttpClient,
        apiUrl = "https://api.betterttv.net/",
        objectMapper = testObjectMapper,
    )

    @Test
    fun loadGlobalEmoticons() = runBlocking<Unit> {
        val emoticons = client.loadGlobalEmoticons().join()
        assertTrue(emoticons.isNotEmpty())
    }

    @Test
    fun loadChannelEmoticons() = runBlocking<Unit> {
        val emoticons = client.loadChannelEmoticons("lirik").join()
        assertTrue(emoticons.isNotEmpty())
    }
}
