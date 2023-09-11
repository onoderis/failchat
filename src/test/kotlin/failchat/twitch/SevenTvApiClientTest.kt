package failchat.twitch

import failchat.assertRequestToUrlReturns200
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Test

class SevenTvApiClientTest {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val apiClient = SevenTvApiClient(
            okHttpClient,
            testObjectMapper
    )

    @Test
    fun loadGlobalEmoticons() = runBlocking {
        val emoticons = apiClient.loadGlobalEmoticons()
        logger.info("7tv global emoticons count: {}", emoticons.size)

        assertRequestToUrlReturns200(emoticons.first().url)
    }

    @Test
    fun loadChannelEmoticons() = runBlocking {
        val emoticons = apiClient.loadChannelEmoticons(23161357L) // lirik
        logger.info("7tv channel emoticons count: {}", emoticons.size)

        assertRequestToUrlReturns200(emoticons.first().url)
    }
}
