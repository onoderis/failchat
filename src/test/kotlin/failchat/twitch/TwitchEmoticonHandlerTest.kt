package failchat.twitch

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import failchat.emoticon.EmoticonFinder
import org.junit.Test

class TwitchEmoticonHandlerTest {

    val emoticonFinder: EmoticonFinder = mock()
    val message: TwitchMessage = mock()
    val firstEmoticon: TwitchEmoticon = mock()
    val secondEmoticon: TwitchEmoticon = mock()

    val handler = TwitchEmoticonHandler(emoticonFinder)

    @Test
    fun longEmoticonCodeTest() {
        whenever(message.text) doReturn("Kappa 123 Kappa Keepo he")
        whenever(message.emotesTag) doReturn("25:0-4,10-14/1902:16-20")
        whenever(emoticonFinder.findById(any(), eq(25L))) doReturn firstEmoticon
        whenever(emoticonFinder.findById(any(), eq(1902L))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn("\${!0}", "\${!1}", "\${!2}")

        handler.handleMessage(message)

        verify(message).text = "\${!0} 123 \${!1} \${!2} he"
    }

    @Test
    fun shortEmoticonCodeTest() {
        whenever(message.text) doReturn("123 :) :-( 321")
        whenever(message.emotesTag) doReturn("1:4-5/2:7-9")
        whenever(emoticonFinder.findById(any(), eq(1L))) doReturn firstEmoticon
        whenever(emoticonFinder.findById(any(), eq(2L))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn("\${!0}", "\${!1}")

        handler.handleMessage(message)

        verify(message).text = "123 \${!0} \${!1} 321"
    }

}
