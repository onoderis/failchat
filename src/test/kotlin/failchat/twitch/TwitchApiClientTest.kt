package failchat.twitch

import failchat.assertRequestToUrlReturns200
import failchat.exception.ChannelNotFoundException
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.okHttpClient
import failchat.testObjectMapper
import failchat.userHomeConfig
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class TwitchApiClientTest {

    private companion object : KLogging() {
        const val userName = "fail_chatbot"
        const val userId = 90826142L
        const val nonExistingUserName = "fail_chatbot2"
    }

    private val apiClient = TwitchApiClient(
            okHttpClient,
            testObjectMapper,
            userHomeConfig.getString("twitch.client-id"),
            userHomeConfig.getString("twitch.client-secret"),
            ConfigurationTokenContainer(userHomeConfig)
    )

    @Test
    fun getUserIdTest() = runBlocking {
        val actualId = apiClient.getUserId(userName)
        assertEquals(userId, actualId)
    }

    @Test
    fun getUserIdNotFoundTest() = runBlocking<Unit> {
        val e = assertFails { apiClient.getUserId(nonExistingUserName) }
        assertIs<ChannelNotFoundException>(e)
    }

    @Test
    fun getFirstLiveChannelName() = runBlocking {
        val userName = apiClient.getFirstLiveChannelName()
        val viewersCount = apiClient.getViewersCount(userName)
        assert(viewersCount >= 0)
    }

    @Test
    fun getViewersCountOfflineTest() = runBlocking<Unit> {
        val e = assertFails { apiClient.getViewersCount(userName) }
        assertIs<ChannelOfflineException>(e)
    }

    @Test
    fun getViewersCountChannelNotFoundTest() = runBlocking<Unit> {
        // if channel is not found the api returns 400 Bad request
        val e = assertFails { apiClient.getViewersCount(nonExistingUserName) }
        assertIs<UnexpectedResponseCodeException>(e)
    }

    @Test
    fun getGlobalEmoticonsTest() = runBlocking {
        val emoticons = apiClient.getGlobalEmoticons()
        assert(emoticons.isNotEmpty())

        assertRequestToUrlReturns200(emoticons.first().url)
    }

    @Test
    fun globalBadgesTest() = runBlocking {
        val badges = apiClient.getGlobalBadges()
        assert(badges.isNotEmpty())
        logger.debug("{} global badges was loaded", badges.size)

        assertRequestToUrlReturns200(badges.values.first().url)
    }

    @Test
    fun channelBadgesTest() = runBlocking {
        val channelId = 23161357L // lirik
        val badges = apiClient.getChannelBadges(channelId)
        assert(badges.isNotEmpty())
        logger.debug("{} channel badges was loaded for channel '{}'", badges.size, channelId)

        assertRequestToUrlReturns200(badges.values.first().url)
    }
}
