package failchat.github

import failchat.defaultConfig
import failchat.okHttpClient
import failchat.testObjectMapper
import org.junit.Test

class GithubClientTest {

    private val githubClient = GithubClient(defaultConfig.getString("github.api-url"), okHttpClient, testObjectMapper)

    @Test
    fun lastReleaseTest() {
        githubClient.requestLatestRelease(
                defaultConfig.getString("github.user-name"),
                defaultConfig.getString("github.repository")
        )
                .join()
    }

}
