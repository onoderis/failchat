package failchat.twitch

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import failchat.core.emoticon.EmoticonManager
import org.junit.Test

class TwitchEmoticonHandlerTest {

    val emoticonManager: EmoticonManager = mock()
    val message: TwitchMessage = mock()
    val firstEmoticon: TwitchEmoticon = mock()
    val secondEmoticon: TwitchEmoticon = mock()

    val handler = TwitchEmoticonHandler(emoticonManager)

    @Test
    fun longEmoticonCodeTest() {
        whenever(message.text) doReturn("Kappa 123 Kappa Keepo he")
        whenever(message.emotesTag) doReturn("25:0-4,10-14/1902:16-20")
        whenever(emoticonManager.find(any(), eq(25L))) doReturn firstEmoticon
        whenever(emoticonManager.find(any(), eq(1902L))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn("\${!0}", "\${!1}", "\${!2}")

        handler.handleMessage(message)

        verify(message).text = "\${!0} 123 \${!1} \${!2} he"
    }

    @Test
    fun shortEmoticonCodeTest() {
        whenever(message.text) doReturn("123 :) :-( 321")
        whenever(message.emotesTag) doReturn("1:4-5/2:7-9")
        whenever(emoticonManager.find(any(), eq(1L))) doReturn firstEmoticon
        whenever(emoticonManager.find(any(), eq(2L))) doReturn secondEmoticon
        whenever(message.addElement(any())).thenReturn("\${!0}", "\${!1}")

        handler.handleMessage(message)

        verify(message).text = "123 \${!0} \${!1} 321"
    }

}