package failchat.youtube

import failchat.Origin
import failchat.chat.Author
import failchat.emoticon.Emoticon
import failchat.okHttpClient
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

class YtEmojiHandlerTest {

    private val emojiHandler = YtEmojiHandler()

    @Test
    fun oneCharacterEmojiTest()  = testYtEmojiHandler("""☕""")

    @Test
    fun twoCharacterEmojiTest()  = testYtEmojiHandler("""😀""")

    @Test
    fun threeCharacterEmojiTest() = testYtEmojiHandler("☝\uD83C\uDFFD")

    @Test
    fun fourCharacterEmojiTest()  = testYtEmojiHandler("👦\uD83C\uDFFD")

    private fun testYtEmojiHandler(text: String) {
        val message = YtMessage(0, "mid", Author("author", Origin.YOUTUBE, "aid"), text)

        emojiHandler.handleMessage(message)

        assertEmojiFound(message)
    }

    private fun assertEmojiFound(message: YtMessage) {
        val request = Request.Builder()
                .get()
                .url((message.elements[0] as Emoticon).url)
                .build()

        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code())
    }

}
