package failchat.twitch

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import failchat.chat.Elements
import failchat.emoticon.EmoticonFinder
import org.junit.Test

class TwitchEmoticonHandlerTest {

    private val emoticonFinder: EmoticonFinder = mock()
    private val message: TwitchMessage = mock()
    private val firstEmoticon: TwitchEmoticon = mock()
    private val secondEmoticon: TwitchEmoticon = mock()

    private val handler = TwitchEmoticonHandler(emoticonFinder)

    @Test
    fun longEmoticonCodeTest() {
        whenever(message.text) doReturn("Kappa 123 Kappa Keepo he")
        whenever(message.emotesTag) doReturn("25:0-4,10-14/1902:16-20")
        whenever(emoticonFinder.findById(any(), eq("25"))) doReturn firstEmoticon
        whenever(emoticonFinder.findById(any(), eq("1902"))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn(Elements.label(0), Elements.label(1), Elements.label(2))

        handler.handleMessage(message)

        verify(message).text = "${Elements.label(0)} 123 ${Elements.label(1)} ${Elements.label(2)} he"
    }

    @Test
    fun shortEmoticonCodeTest() {
        whenever(message.text) doReturn("123 :) :-( 321")
        whenever(message.emotesTag) doReturn("1:4-5/2:7-9")
        whenever(emoticonFinder.findById(any(), eq("1"))) doReturn firstEmoticon
        whenever(emoticonFinder.findById(any(), eq("2"))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn(Elements.label(0), Elements.label(1))

        handler.handleMessage(message)

        verify(message).text = "123 ${Elements.label(0)} ${Elements.label(1)} 321"
    }

}
