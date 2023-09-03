package failchat.twitch

import failchat.chat.Elements
import io.kotest.matchers.shouldBe
import org.junit.Test

class TwitchEmoticonHandlerTest {

    private val handler = TwitchEmoticonHandler(TwitchEmotesTagParser())

    @Test
    fun longEmoticonCodeTest() {
        // Given
        val message = TwitchMessage(
                id = 0,
                author = "",
                text = "Kappa 123 Kappa Keepo he",
                tags = mapOf(TwitchIrcTags.emotes to "25:0-4,10-14/1902:16-20")
        )

        // When
        handler.handleMessage(message)

        // Then
        message.text shouldBe "${Elements.label(0)} 123 ${Elements.label(1)} ${Elements.label(2)} he"
        message.elements.size shouldBe 3
        (message.elements[0] as TwitchEmoticon).twitchId shouldBe "25"
        (message.elements[0] as TwitchEmoticon).code shouldBe "Kappa"
        (message.elements[1] as TwitchEmoticon).twitchId shouldBe "25"
        (message.elements[1] as TwitchEmoticon).code shouldBe "Kappa"
        (message.elements[2] as TwitchEmoticon).twitchId shouldBe "1902"
        (message.elements[2] as TwitchEmoticon).code shouldBe "Keepo"
    }

    @Test
    fun noEmoticonsTest() {
        // Given
        val message = TwitchMessage(
                id = 0,
                author = "",
                text = "message",
                tags = mapOf(TwitchIrcTags.emotes to "")
        )
        // When
        handler.handleMessage(message)

        // Then
        message.elements.size shouldBe 0
        message.text shouldBe "message"
    }

}
