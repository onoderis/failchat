package failchat.github

import failchat.config
import failchat.okHttpClient
import failchat.testObjectMapper
import org.junit.Test

class GithubClientTest {

    private val githubClient = GithubClient(config.getString("github.api-url"), okHttpClient, testObjectMapper)


    @Test
    fun lastReleaseTest() {
        githubClient.requestLatestRelease(
                config.getString("github.user-name"),
                config.getString("github.repository")
        )
                .join()
    }

}
