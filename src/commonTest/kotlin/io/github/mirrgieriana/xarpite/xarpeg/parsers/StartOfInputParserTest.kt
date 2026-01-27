package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StartOfInputParserTest {

    @Test
    fun startOfInputMatchesAtPositionZero() {
        val parser = startOfInput
        val context = ParseContext("hello", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun startOfInputFailsAtNonZeroPosition() {
        val parser = startOfInput
        val context = ParseContext("hello", useMemoization = true)
        val result = parser.parseOrNull(context, 1)
        assertNull(result)
    }

    @Test
    fun startOfInputDoesNotConsumeInput() {
        val parser = startOfInput * +"hello"
        val result = parser.parseAllOrThrow("hello")
        assertEquals("hello", result)
    }

    @Test
    fun startOfInputInSequence() {
        val parser = startOfInput * +'a' * +'b'
        val result = parser.parseAllOrThrow("ab")
        assertEquals('a', result.a)
        assertEquals('b', result.b)
    }

    @Test
    fun startOfInputWithRegex() {
        val parser = startOfInput * +Regex("[0-9]+") map { it.value }
        val result = parser.parseAllOrThrow("123")
        assertEquals("123", result)
    }

    @Test
    fun startOfInputOnEmptyString() {
        val parser = startOfInput
        val context = ParseContext("", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun multipleStartOfInputParsers() {
        val parser = startOfInput * startOfInput * +"test"
        val result = parser.parseAllOrThrow("test")
        assertEquals("test", result)
    }

    @Test
    fun startOfInputWithOptional() {
        val parser = startOfInput * (+'a').optional * +'b'
        val result1 = parser.parseAllOrThrow("ab")
        assertEquals('a', result1.a)
        assertEquals('b', result1.b)

        val result2 = parser.parseAllOrThrow("b")
        assertNull(result2.a)
        assertEquals('b', result2.b)
    }
}
