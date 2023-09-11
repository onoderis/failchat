package failchat.youtube

import failchat.ktorClient
import failchat.testObjectMapper
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue

@Ignore
class YoutubeClientTest {

    private val client = YoutubeClient(
            httpClient = ktorClient,
            objectMapper = testObjectMapper,
            youtubeHtmlParser = YoutubeHtmlParser(objectMapper = testObjectMapper)
    )
    private val videoId = "jfKfPfyJRdk"

    @Test
    fun getViewersCountTest() = runBlocking<Unit> {
        val innertubeApiKey = client.getNewLiveChatSessionData(videoId).innertubeApiKey

        val count = client.getViewersCount(videoId, innertubeApiKey)

        count shouldBeGreaterThanOrEqual 0
        println(count)
    }

    @Test
    fun getMessagesTest() = runBlocking<Unit> {
        val params = client.getNewLiveChatSessionData(videoId)
        val response = client.getLiveChatResponse(params)

        assertTrue(response.continuationContents.liveChatContinuation.actions.isNotEmpty())
    }
}
