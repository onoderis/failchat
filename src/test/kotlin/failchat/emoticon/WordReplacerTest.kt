package failchat.emoticon

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class WordReplacerTest {

    @Test
    fun replaceNothingTest() {
        val words = listOf("just", "a", "simple", "message")
        val initialString = words.joinToString(separator = " ")

        val actualWords = ArrayList<String>()
        val resultString = WordReplacer.replace(initialString) {
            actualWords.add(it)
            ReplaceDecision.Skip
        }

        assertSame(initialString, resultString)
        assertEquals(words, actualWords)
    }

    @Test
    fun replaceAllTest() {
        val resultString = WordReplacer.replace("just a simple message") {
            ReplaceDecision.Replace("42")
        }

        assertEquals("42 42 42 42", resultString)
    }

    @Test
    fun specialCharactersTest() {
        val words = listOf(
                "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
                "!!", "@@", "##", "$$", "%%", "^^", "&&", "**", "((", "))"
        )
        val initialString = words.joinToString(separator = " ")

        val resultString = WordReplacer.replace(initialString) {
            ReplaceDecision.Replace("1")
        }

        assertEquals(words.joinToString(separator = " ") { "1" }, resultString)
    }

    @Test
    fun lineBreakTest() {
        val resultString = WordReplacer.replace("the\nmessage") {
            ReplaceDecision.Replace("1")
        }

        assertEquals("1\n1", resultString)
    }

}
