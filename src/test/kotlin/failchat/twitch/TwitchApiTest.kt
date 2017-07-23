package failchat.twitch

import failchat.exceptions.ChannelOfflineException
import failchat.utils.loadConfig
import failchat.utils.okHttpClient
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletionException
import kotlin.system.measureTimeMillis

@Ignore
class TwitchApiTest {

    companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchApiTest::class.java)
        val config = loadConfig()
        val timeout: Duration = Duration.ofSeconds(50)
        val userNames = setOf("lirik", "Doublelift", "C9Sneaky", "TSM_Dyrus", "MOONMOON_OW", "aimbotcalvin")
        val userIds = setOf<Long>(23161357, 40017619, 24538518, 30080751, 121059319, 84574550)
    }

    val apiClient = TwitchApiClient(
            okHttpClient,
            config.getString("twitch.api-url"),
            config.getString("twitch.api-token")
    )

    @Test
    fun requestUserIdTest() {
        userNames.forEach { userName ->
            val userId = apiClient.requestUserId(userName).join()
            log.debug("user id: {}", userId)
        }
    }

    @Test
    fun requestViewersCountTest() {
        userIds.forEach { userId ->
            try {
                val count = apiClient.requestViewersCount(userId).join()
                log.debug("count: {}", count)
            } catch (e: CompletionException) {
                if (e.cause is ChannelOfflineException) {
                    log.debug("user with id {} is offline", userId)
                } else {
                    throw e
                }
            }
        }
    }

    @Test
    fun requestEmoticonsTest() {
        val time = measureTimeMillis {
            val size = apiClient.requestEmoticons().join().size
            log.debug("emoticons: {}", size)
        }
        log.debug("emoticons loaded in {} ms", time)

    }

}