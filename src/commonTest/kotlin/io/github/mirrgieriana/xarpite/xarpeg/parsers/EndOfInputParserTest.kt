package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.assertUnmatchedInput
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EndOfInputParserTest {

    @Test
    fun endOfInputMatchesAtEndOfString() {
        val parser = endOfInput
        val context = ParseContext("hello", useMemoization = true)
        val result = parser.parseOrNull(context, 5)
        assertNotNull(result)
        assertEquals(5, result.start)
        assertEquals(5, result.end)
    }

    @Test
    fun endOfInputFailsBeforeEndOfString() {
        val parser = endOfInput
        val context = ParseContext("hello", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun endOfInputDoesNotConsumeInput() {
        val parser = +"hello" * endOfInput
        val result = parser.parseAllOrThrow("hello")
        assertEquals("hello", result)
    }

    @Test
    fun endOfInputInSequence() {
        val parser = +'a' * +'b' * endOfInput
        val result = parser.parseAllOrThrow("ab")
        assertEquals('a', result.a)
        assertEquals('b', result.b)
    }

    @Test
    fun endOfInputWithRegex() {
        val parser = +Regex("[0-9]+") * endOfInput map { it.value }
        val result = parser.parseAllOrThrow("123")
        assertEquals("123", result)
    }

    @Test
    fun endOfInputOnEmptyString() {
        val parser = endOfInput
        val context = ParseContext("", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun multipleEndOfInputParsers() {
        val parser = +"test" * endOfInput * endOfInput
        val result = parser.parseAllOrThrow("test")
        assertEquals("test", result)
    }

    @Test
    fun endOfInputDetectsTrailingCharacters() {
        val parser = +"hello" * endOfInput
        assertUnmatchedInput { parser.parseAllOrThrow("hello world") }
    }

    @Test
    fun endOfInputWithOptional() {
        val parser = +'a' * (+'b').optional * endOfInput
        val result1 = parser.parseAllOrThrow("ab")
        assertEquals('a', result1.a)
        assertEquals('b', result1.b)

        val result2 = parser.parseAllOrThrow("a")
        assertEquals('a', result2.a)
        assertNull(result2.b)
    }

    @Test
    fun startAndEndOfInputTogether() {
        val parser = startOfInput * +"test" * endOfInput
        val result = parser.parseAllOrThrow("test")
        assertEquals("test", result)
    }
}
