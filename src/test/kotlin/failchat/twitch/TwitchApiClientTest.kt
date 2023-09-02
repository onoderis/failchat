package failchat.twitch

import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import failchat.testObjectMapper
import failchat.twitchEmoticonUrlFactory
import failchat.userHomeConfig
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
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
            userHomeConfig.getString("twitch.client-id"),
            userHomeConfig.getString("twitch.client-secret"),
            twitchEmoticonUrlFactory,
            ConfigurationTokenContainer(userHomeConfig)
    )

    @Test
    @Ignore
    fun requestUserIdTest() {
        userNameToId.forEach { (name, expectedId) ->
            val actualId = apiClient.getUserId(name).join()
            assertEquals(expectedId, actualId)
        }
    }

    @Test
    @Ignore
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
    @Ignore
    fun getCommonEmoticonsTest() {
        val emoticons = apiClient.getCommonEmoticons().join()
        emoticons shouldNotBe 0
    }

    @Test
    fun globalBadgesTest() = runBlocking {
        val badges = apiClient.getGlobalBadges()
        assert(badges.isNotEmpty())
        log.debug("{} global badges was loaded", badges.size)
    }

    @Test
    fun channelBadgesTest() = runBlocking {
        val channelId = userNameToId.values.first()
        val badges = apiClient.getChannelBadges(channelId)
        assert(badges.isNotEmpty())
        log.debug("{} channel badges was loaded for channel '{}'", badges.size, channelId)
    }
}
