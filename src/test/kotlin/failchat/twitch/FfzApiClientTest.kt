package failchat.twitch

import failchat.ConfigKeys
import failchat.config
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.junit.Test

class FfzApiClientTest {

    private companion object : KLogging()

    private val apiClient = FfzApiClient(
            okHttpClient,
            testObjectMapper,
            config.getString(ConfigKeys.frankerfacezApiUrl)
    )
    private val roomName = "forsen"

    @Test
    fun requestEmoticonsTest(): Unit = runBlocking {
        val emoticons = apiClient.requestEmoticons(roomName)
        logger.info("ffz emoticons for room {} - {}", roomName, emoticons.size)
    }

}
