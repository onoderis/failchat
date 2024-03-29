package failchat.goodgame

import failchat.defaultConfig
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class GgApiClientTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GgApiClientTest::class.java)
    }

    private val apiClient = GgApiClient(
            okHttpClient,
            testObjectMapper,
            defaultConfig.getString("goodgame.api-url"),
            defaultConfig.getString("goodgame.emoticon-js-url")
    )

    @Test
    fun channelIdTest() = runBlocking<Unit> {
        apiClient.requestChannelId("Miker")
    }

    @Test
    fun emoticonsRequestTest() = runBlocking<Unit> {
        val t = measureTimeMillis {
            val emoticons = apiClient.requestEmoticonList()
            log.debug("gg emoticons size: {}", emoticons.size)
        }
        log.debug("gg emoticons load time: {} ms", t)
    }

    @Test
    fun requestChannelInfoTest() = runBlocking<Unit> {
        val c = apiClient.requestChannelInfo("Miker")
        log.debug("Channel info: {}", c)
    }

}
