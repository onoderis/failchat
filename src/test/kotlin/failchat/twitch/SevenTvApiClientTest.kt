package failchat.twitch

import failchat.okHttpClient
import failchat.testObjectMapper
import failchat.util.await
import kotlinx.coroutines.runBlocking
import mu.KLogging
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

class SevenTvApiClientTest {

    private companion object : KLogging()

    private val apiClient = SevenTvApiClient(
            okHttpClient,
            testObjectMapper
    )

    @Test
    fun loadGlobalEmoticons() = runBlocking {
        val emoticons = apiClient.loadGlobalEmoticons()
        logger.info("7tv global emoticons count: {}", emoticons.size)

        assertEmoteIsRetrievable(emoticons.first())
    }

    @Test
    fun loadChannelEmoticons() = runBlocking {
        val emoticons = apiClient.loadChannelEmoticons(23161357L) // lirik
        logger.info("7tv channel emoticons count: {}", emoticons.size)

        assertEmoteIsRetrievable(emoticons.first())
    }

    private suspend fun assertEmoteIsRetrievable(emote: SevenTvEmoticon) {
        val request = Request.Builder().url(emote.url).get().build()
        okHttpClient.newCall(request).await().use {
            assertEquals(200, it.code)
        }
    }
}
