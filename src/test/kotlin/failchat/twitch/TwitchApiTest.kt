package failchat.twitch

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import failchat.privateConfig
import failchat.twitchEmoticonUrlFactory
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionException
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class TwitchApiTest {

    private companion object {
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
            config.getString("twitch.badge-api-url"),
            privateConfig.getString("twitch.api-token"),
            twitchEmoticonUrlFactory
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

    @Test
    fun globalBadgesTest() = runBlocking {
        val badges = apiClient.requestGlobalBadges()
        log.debug("{} global badges was loaded", badges.size)
    }

    @Test
    fun channelBadgesTest() = runBlocking {
        val channelId = userNameToId.values.first()
        val badges = apiClient.requestChannelBadges(channelId)
        log.debug("{} channel badges was loaded for channel '{}'", badges.size, channelId)
    }

}
