package failchat.goodgame

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class GgApiTest {

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
        apiClient.requestEmoticonList()
    }

    @Test
    fun viewersCountTest() = runBlocking<Unit> {
        try {
            apiClient.requestViewersCount("Miker")
        } catch (ignored: ChannelOfflineException) {
        }
    }

}
