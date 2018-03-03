package failchat.goodgame

import failchat.config
import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class GgApiTest {

    private val apiClient = GgApiClient(
            okHttpClient,
            config.getString("goodgame.api-url"),
            config.getString("goodgame.emoticon-js-url")
    )

    @Test
    fun emoticonsRequestTest() {
        apiClient.requestEmoticonList().join()
    }

    @Test
    fun viewersCountTest() = runBlocking<Unit> {
        try {
            apiClient.requestViewersCount("Miker").await()
        } catch (ignored: ChannelOfflineException) {
        }
    }

    @Test
    fun channelIdTest() {
        apiClient.requestChannelId("Miker").join()
    }

}
