package failchat.youtube2

import failchat.ktorClient
import failchat.testObjectMapper
import failchat.youtube.YoutubeClient
import failchat.youtube.YoutubeHtmlParser
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@Ignore
class YoutubeClientTest {

    private val client = YoutubeClient(
            httpClient = ktorClient,
            objectMapper = testObjectMapper,
            youtubeHtmlParser = YoutubeHtmlParser(objectMapper = testObjectMapper)
    )

    @Test
    fun getViewersCount() = runBlocking<Unit> {
        val videoId = "HsGYQKQxPBs"
        val innertubeApiKey = client.getNewLiveChatSessionData(videoId).innertubeApiKey

        val count = client.getViewersCount(videoId, innertubeApiKey)

        count shouldBeGreaterThanOrEqual 0
        println(count)
    }

}
