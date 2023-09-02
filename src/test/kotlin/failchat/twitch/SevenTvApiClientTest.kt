package failchat.twitch

import failchat.ConfigKeys
import failchat.defaultConfig
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.junit.Test

class SevenTvApiClientTest {

    private companion object : KLogging()

    private val apiClient = SevenTvApiClient(
            okHttpClient,
            defaultConfig.getString(ConfigKeys.sevenTvApiUrl),
            testObjectMapper
    )

    @Test
    fun loadGlobalEmoticons() = runBlocking<Unit> {
        val emoticons = apiClient.loadGlobalEmoticons()
        logger.info("7tv global emoticons count: {}", emoticons.size)
    }

    @Test
    fun loadChannelEmoticons() = runBlocking<Unit> {
        val emoticons = apiClient.loadChannelEmoticons("ch0bot")
        logger.info("7tv channel emoticons count: {}", emoticons.size)
    }

}
