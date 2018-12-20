package failchat.twitch

import failchat.config
import failchat.okHttpClient
import failchat.twitchEmoticonUrlFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class TwitchemotesApiClientTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchemotesApiClientTest::class.java)
    }

    private val client = TwitchemotesApiClient(
            okHttpClient,
            config.getString("twitch.twitchemotes-api-url"),
            twitchEmoticonUrlFactory
    )

    @Test
    fun requestAllEmoticonsTest() {
        var size: Int? = null
        val time = measureTimeMillis {
            size = runBlocking { client.requestAllEmoticons().size }
            log.debug("emoticons: {}", size)
        }
        log.debug("emoticons loaded in {} ms. size: {}", time, size)
    }

    @Test
    fun requestGlobalEmoticonsTest() {
        var size: Int? = null
        val time = measureTimeMillis {
            size = runBlocking { client.requestGlobalEmoticons().size }
            log.debug("emoticons: {}", size)
        }
        log.debug("emoticons loaded in {} ms. size: {}", time, size)
    }
}
