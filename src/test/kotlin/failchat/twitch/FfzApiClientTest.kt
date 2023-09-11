package failchat.twitch

import failchat.ConfigKeys
import failchat.defaultConfig
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Test

class FfzApiClientTest {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val apiClient = FfzApiClient(
            okHttpClient,
            testObjectMapper,
            defaultConfig.getString(ConfigKeys.frankerfacezApiUrl)
    )
    private val roomName = "forsen"

    @Test
    fun requestEmoticonsTest(): Unit = runBlocking {
        val emoticons = apiClient.requestEmoticons(roomName)
        logger.info("ffz emoticons for room {} - {}", roomName, emoticons.size)
    }

}
