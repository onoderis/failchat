package failchat.peka2tv

import failchat.config
import failchat.okHttpClient
import failchat.testObjectMapper
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Peka2tvApiTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvApiTest::class.java)
    }

    private val apiClient = Peka2tvApiClient(okHttpClient, testObjectMapper, config.getString("peka2tv.api-url"))

    @Test
    fun emoticonApiTest() {
        apiClient
                .requestEmoticons()
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
