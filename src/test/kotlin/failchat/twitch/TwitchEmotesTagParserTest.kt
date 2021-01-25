package failchat.twitch

import io.mockk.mockk
import org.junit.Test

class TwitchEmotesTagParserTest {

    private val twitchEmoticonUrlFactory: TwitchEmoticonUrlFactory = mockk()
    private val parser: TwitchEmotesTagParser = TwitchEmotesTagParser(twitchEmoticonUrlFactory)

    @Test
    fun parseTest() {
        println("ok")
    }


}
