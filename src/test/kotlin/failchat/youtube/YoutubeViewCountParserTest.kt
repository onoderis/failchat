package failchat.youtube

import io.kotest.matchers.shouldBe
import org.junit.Test

class YoutubeViewCountParserTest {

    private val youtubeViewCountParser = YoutubeViewCountParser()

    @Test
    fun `should parse number less than 1000`() {
        // Given
        val viewCount = "1,232 watching now"
        // When
        val actual = youtubeViewCountParser.parse(viewCount)

        // Then
        actual shouldBe 1232
    }

    @Test
    fun `should parse number greater or equals than 1000`() {
        // Given
        val viewCount = "609 watching now"

        // When
        val actual = youtubeViewCountParser.parse(viewCount)

        // Then
        actual shouldBe 609
    }

    //todo zero viewers

}
