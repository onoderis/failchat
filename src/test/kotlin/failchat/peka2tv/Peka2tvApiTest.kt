package failchat.peka2tv

import failchat.config
import failchat.util.get
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.test.assertTrue

@Ignore
class Peka2tvApiTest {

    companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvApiTest::class.java)
        val timeout: Duration = Duration.ofSeconds(5)
    }

    private val apiClient = Peka2tvApiClient(OkHttpClient(), config.getString("peka2tv.api-url"))

    @Test
    fun emoticonApiTest() {
        apiClient
                .request("/smile")
                .thenAccept {
                    it.forEach { emoticonNode ->
                        assertTrue { emoticonNode.get("code").isTextual }
                        assertTrue { emoticonNode.get("url").isTextual }
                    }
                }
                .get(timeout)
    }

    @Test
    fun userApiTest() {
        apiClient
                .findUser("akudji")
                .get(timeout)
    }

}
