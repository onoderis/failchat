package failchat.handlers

import failchat.emoticon.ReplaceDecision
import failchat.emoticon.SemicolonCodeProcessor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SemicolonCodeProcessorTest {

    @Test
    fun emptyString() {
        val initialString = ""
        val processedString = SemicolonCodeProcessor.process(initialString) {
            error("shouldn't be invoked")
        }
        assertSame(initialString, processedString)
    }

    @Test
    fun justSemicolons() {
        val initialString = ":::::::::"
        val processedString = SemicolonCodeProcessor.process(initialString) {
            error("shouldn't be invoked")
        }
        assertSame(initialString, processedString)
    }

    @Test
    fun replaceSingleCode() {
        val processedString = SemicolonCodeProcessor.process(":code:") {
            assertEquals("code", it)
            ReplaceDecision.Replace("42")
        }

        assertEquals("42", processedString)
    }

    @Test
    fun replaceSingleCodeAroundText() {
        val processedString = SemicolonCodeProcessor.process("text1 :code: text2") {
            assertEquals("code", it)
            ReplaceDecision.Replace("42")
        }

        assertEquals("text1 42 text2", processedString)
    }


    @Test
    fun skipSingleCode() {
        val initialString = ":code:"

        val processedString = SemicolonCodeProcessor.process(initialString) {
            assertEquals("code", it)
            ReplaceDecision.Skip
        }

        assertSame(initialString, processedString)
    }

    @Test
    fun replaceMultipleCodes() {
        val codes = mutableListOf<String>()

        val processedString = SemicolonCodeProcessor.process("hello :one: :two: :)") {
            codes.add(it)
            ReplaceDecision.Replace("1")
        }

        assertEquals(listOf("one", "two"), codes)
        assertEquals("hello 1 1 :)", processedString)
    }

    @Test
    fun semicolonHell() {
        val codes = mutableListOf<String>()

        val processedString = SemicolonCodeProcessor.process(":w:wrong:right:w:w:") {
            codes.add(it)
            if (it == "right") ReplaceDecision.Replace("1") else ReplaceDecision.Skip
        }

        assertEquals(listOf("w", "wrong", "right", "w"), codes)
        assertEquals(":w:wrong1w:w:", processedString)
    }

}
