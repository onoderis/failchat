package failchat.twitch

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import failchat.privateConfig
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionException
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class TwitchApiTest {

    companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchApiTest::class.java)
        val userNameToId: Map<String, Long> = mapOf(
                "lirik" to 23161357L,
                "Doublelift" to 40017619L,
                "TSM_Dyrus" to 6356773L,
                "C9Sneaky" to 24538518L,
                "MOONMOON_OW" to 121059319L,
                "aimbotcalvin" to 84574550L
        )
    }

    private val apiClient = TwitchApiClient(
            okHttpClient,
            config.getString("twitch.api-url"),
            privateConfig.getString("twitch.api-token")
    )

    @Test
    fun requestUserIdTest() {
        userNameToId.forEach { (name, expectedId) ->
            val actualId = apiClient.requestUserId(name).join()
            assertEquals(expectedId, actualId)
        }
    }

    @Test
    fun requestViewersCountTest() {
        userNameToId.forEach { (_, id) ->
            try {
                // todo investigate why coroutines doesn't unwrap CompletionException
                apiClient.requestViewersCount(id).join()
            } catch (e: CompletionException) {
                if (e.cause !is ChannelOfflineException) throw e
            }
        }
    }

    @Test
    @Ignore //because of read timeout
    fun requestEmoticonsTest() {
        var size: Int? = null
        val time = measureTimeMillis {
            size = apiClient.requestEmoticons().join().size
            log.debug("emoticons: {}", size)
        }
        log.debug("emoticons loaded in {} ms. size: {}", time, size)
    }

}
