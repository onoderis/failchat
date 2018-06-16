package failchat.peka2tv

import failchat.config
import failchat.okHttpClient
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class Peka2tvApiTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvApiTest::class.java)
    }

    private val apiClient = Peka2tvApiClient(okHttpClient, config.getString("peka2tv.api-url"))

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
                .join()
    }

    @Test
    fun userApiTest() {
        apiClient
                .findUser("akudji")
                .join()
    }

    @Test
    fun requestBadgesTest() {
        val badges = apiClient
                .requestBadges()
                .join()

        log.debug("Peka2tv Badges was loaded. count: {}", badges.size)
    }

}
