package failchat.github

import failchat.config
import failchat.objectMapper
import failchat.okHttpClient
import org.junit.Test

class GithubClientTest {

    private val githubClient = GithubClient(config.getString("github.api-url"), okHttpClient, objectMapper)


    @Test
    fun lastReleaseTest() {
        githubClient.requestLatestRelease(
                config.getString("github.user-name"),
                config.getString("github.repository")
        )
                .join()
    }

}
