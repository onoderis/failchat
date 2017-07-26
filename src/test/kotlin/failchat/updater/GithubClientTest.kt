package failchat.updater

import failchat.github.GithubClient
import failchat.utils.config
import failchat.utils.objectMapper
import failchat.utils.okHttpClient
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GithubClientTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GithubClientTest::class.java)
    }

    val githubClient = GithubClient(config.getString("github.api-url"), okHttpClient, objectMapper)


    @Test
    fun lastReleaseTest() {
        val release = githubClient.requestLatestRelease("sph-u", "failchat").join()

        log.info(release.version.toString())
    }

}
