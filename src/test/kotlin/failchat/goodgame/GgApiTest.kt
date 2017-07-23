package failchat.goodgame

import failchat.utils.loadConfig
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

@Ignore
class GgApiTest {

    companion object {
        val log: Logger = LoggerFactory.getLogger(GgApiTest::class.java)
        val timeout: Duration = Duration.ofSeconds(50)
        val config = loadConfig()
    }

    val apiClient = GgApiClient(
            OkHttpClient(),
            config.getString("goodgame.api-url"),
            config.getString("goodgame.emoticon-js-url")
    )

    @Test
    fun emoticonsTest() {
        val emoticons = apiClient.requestEmoticonList().join()
        log.debug("Emoticons loaded: {}", emoticons.size)
    }

    @Test
    fun viewersCountTest() {
//        val count = apiClient.requestViewersCount("Miker").join()
        val count = apiClient.requestViewersCount("ZERG").join()
        log.debug("Viewers count: {}", count)
    }

    @Test
    fun channelIdTest() {
        val channelId = apiClient.requestChannelId("fail0001").join()
        log.debug("Channel id: {}", channelId)
    }

}