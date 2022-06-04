package failchat.chat.handlers

import failchat.Origin
import failchat.chat.Author
import failchat.emoticon.Emoticon
import failchat.okHttpClient
import failchat.youtube.YoutubeMessage
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

class EmojiHandlerTest {

    private val emojiHandler = EmojiHandler()

    @Test
    fun oneCharacterEmojiTest() = testYtEmojiHandler("""☕""")

    @Test
    fun twoCharacterEmojiTest() = testYtEmojiHandler("""😀""")

    @Test
    fun threeCharacterEmojiTest() = testYtEmojiHandler("☝\uD83C\uDFFD")

    @Test
    fun fourCharacterEmojiTest() = testYtEmojiHandler("👦\uD83C\uDFFD")

    @Test
    fun flagTest() = testYtEmojiHandler("🇦🇩")

    private fun testYtEmojiHandler(text: String) {
        val message = YoutubeMessage(0, Author("author", Origin.YOUTUBE, "aid"), text)

        emojiHandler.handleMessage(message)

        assertEmojiFound(message)
    }

    private fun assertEmojiFound(message: YoutubeMessage) {
        val request = Request.Builder()
                .get()
                .url((message.elements[0] as Emoticon).url)
                .build()

        okHttpClient.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
    }

}
