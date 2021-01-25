package failchat.youtube2

import failchat.readResourceAsString
import failchat.testObjectMapper
import failchat.youtube.YoutubeClientException
import failchat.youtube.YoutubeHtmlParser
import io.kotest.matchers.shouldBe
import org.junit.Test

class YoutubeHtmlParserTest {

    private val youtubeHtmlParser = YoutubeHtmlParser(testObjectMapper)

    @Test
    fun `extractSessionId should extract from innertube context that is set as separate statement`() {
        // Given
        val html = readResourceAsString("/html/live_chat-multiple-config-statements.html")

        // When
        val actual = youtubeHtmlParser.extractSessionId(html)

        // Then
        actual shouldBe "6861449898531449659"
    }

    @Test
    fun `extractSessionId should extract from innertube context that is set in the common config`() {
        // Given
        val html = readResourceAsString("/html/live_chat-one-config-statement.html")

        // When
        val actual = youtubeHtmlParser.extractSessionId(html)

        // Then
        actual shouldBe "6921135002565427170"
    }

    @Test(expected = YoutubeClientException::class)
    fun `extractSessionId should fail if session id is not found`() {
        // Given, When, Then
        youtubeHtmlParser.extractSessionId("")
    }

}
