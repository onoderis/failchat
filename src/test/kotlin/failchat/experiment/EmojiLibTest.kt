package failchat.experiment

import com.vdurmont.emoji.EmojiParser
import org.junit.Ignore
import org.junit.Test

@Ignore
class EmojiLibTest {
    @Test
    fun test() {
        /*
         * ☕ -> 2615
         * 😀 -> 1f600
         * ☝🏽 -> 261d-1f3fd
         * 👦🏽 -> 1f466-1f3fd
         * todo 🧘‍♀
         * */

        EmojiParser.parseFromUnicode("\uD83D\uDE00") {
            println(it.emoji.unicode)
            "<>"
        }
    }
}
