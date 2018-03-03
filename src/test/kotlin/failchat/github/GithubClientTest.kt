package failchat.github

import failchat.config
import failchat.objectMapper
import failchat.okHttpClient
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
        val release = githubClient.requestLatestRelease(
                config.getString("github.user-name"),
                config.getString("github.repository")
        )
                .join()

        log.info(release.version.toString())
    }

}
