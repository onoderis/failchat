package failchat.handlers

import failchat.chat.handlers.SemicolonCodeProcessor
import failchat.chat.handlers.SemicolonCodeProcessor.Decision
import org.junit.Test
import kotlin.test.assertEquals

class SemicolonCodeProcessorTest {

    @Test
    fun emptyString() {
        val processedString = SemicolonCodeProcessor.process("") {
            error("shouldn't be invoked")
        }
        assertEquals("", processedString)
    }

    @Test
    fun justSemicolons() {
        val processedString = SemicolonCodeProcessor.process(":::::::::") {
            error("shouldn't be invoked")
        }
        assertEquals(":::::::::", processedString)
    }

    @Test
    fun replaceSingleCode() {
        val processedString = SemicolonCodeProcessor.process(":code:") {
            assertEquals("code", it)
            Decision.Replace("42")
        }

        assertEquals("42", processedString)
    }

    @Test
    fun replaceSingleCodeAroundText() {
        val processedString = SemicolonCodeProcessor.process("text1 :code: text2") {
            assertEquals("code", it)
            Decision.Replace("42")
        }

        assertEquals("text1 42 text2", processedString)
    }


    @Test
    fun skipSingleCode() {
        val processedString = SemicolonCodeProcessor.process(":code:") {
            assertEquals("code", it)
            Decision.Skip
        }

        assertEquals(":code:", processedString)
    }

    @Test
    fun replaceMultipleCodes() {
        val codes = mutableListOf<String>()

        val processedString = SemicolonCodeProcessor.process("hello :one: :two: :)") {
            codes.add(it)
            Decision.Replace("1")
        }

        assertEquals(listOf("one", "two"), codes)
        assertEquals("hello 1 1 :)", processedString)
    }

    @Test
    fun semicolonHell() {
        val codes = mutableListOf<String>()

        val processedString = SemicolonCodeProcessor.process(":w:wrong:right:w:w:") {
            codes.add(it)
            if (it == "right") Decision.Replace("1") else Decision.Skip
        }

        assertEquals(listOf("w", "wrong", "right", "w"), codes)
        assertEquals(":w:wrong1w:w:", processedString)
    }


}
