package failchat.goodgame

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class GgApiTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GgApiTest::class.java)
    }

    private val apiClient = GgApiClient(
            okHttpClient,
            config.getString("goodgame.api-url"),
            config.getString("goodgame.emoticon-js-url")
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
    fun viewersCountTest() = runBlocking<Unit> {
        try {
            apiClient.requestViewersCount("Miker")
        } catch (ignored: ChannelOfflineException) {
        }
    }

    @Test
    fun requestChannelInfoTest() = runBlocking<Unit> {
        val c = apiClient.requestChannelInfo("Miker")
        log.debug("Channel info: {}", c)
    }

}
