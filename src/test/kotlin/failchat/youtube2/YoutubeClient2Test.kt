package failchat.youtube2

import failchat.ktorClient
import failchat.objectMapper
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@Ignore
class YoutubeClient2Test {

    private val client = YoutubeClient2(
            httpClient = ktorClient,
            objectMapper = objectMapper,
            youtubeHtmlParser = YoutubeHtmlParser(objectMapper = objectMapper)
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
