package failchat.twitch

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import failchat.privateConfig
import failchat.testObjectMapper
import failchat.twitchEmoticonUrlFactory
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionException
import kotlin.test.assertEquals

class TwitchApiClientTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchApiClientTest::class.java)
        val userNameToId: Map<String, Long> = mapOf(
                "lirik" to 23161357L,
                "Doublelift" to 40017619L,
                "TSM_Dyrus" to 6356773L,
                "aimbotcalvin" to 84574550L
        )
    }

    private val apiClient = TwitchApiClient(
            okHttpClient,
            testObjectMapper,
            config.getString("twitch.api-url"),
            config.getString("twitch.badge-api-url"),
            privateConfig.getString("twitch.api-token"),
            twitchEmoticonUrlFactory
    )

    @Test
    fun requestUserIdTest() {
        userNameToId.forEach { (name, expectedId) ->
            val actualId = apiClient.getUserId(name).join()
            assertEquals(expectedId, actualId)
        }
    }

    @Test
    fun requestViewersCountTest() {
        userNameToId.forEach { (_, id) ->
            try {
                apiClient.getViewersCount(id).join()
            } catch (e: CompletionException) {
                if (e.cause !is ChannelOfflineException) throw e
            }
        }
    }

    @Test
    fun getCommonEmoticonsTest() {
        val emoticons = apiClient.getCommonEmoticons().join()
        emoticons shouldNotBe 0
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
