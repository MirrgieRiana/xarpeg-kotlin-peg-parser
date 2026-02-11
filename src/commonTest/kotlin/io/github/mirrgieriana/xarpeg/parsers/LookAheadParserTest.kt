package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * LookAheadパーサーの動作をテストする
 */
class LookAheadParserTest {

    @Test
    fun lookaheadParserMatchesWithoutConsuming() {
        val parser = (+'a').lookAhead
        val context = ParseContext("abc", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals('a', result.value)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun lookaheadParserFailsWhenInnerParserFails() {
        val parser = (+'a').lookAhead
        val context = ParseContext("bcd", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun lookaheadParserInSequence() {
        val parser = (+'a').lookAhead * +'a' * +'b'
        val result = parser.parseAllOrThrow("ab")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('b', result.c)
    }

    @Test
    fun lookaheadParserWithString() {
        val parser = (+"hello").lookAhead * +"hello" * +"world"
        val result = parser.parseAllOrThrow("helloworld")
        assertEquals("hello", result.a)
        assertEquals("hello", result.b)
        assertEquals("world", result.c)
    }

    @Test
    fun lookaheadParserDoesNotConsumeInput() {
        val parser = (+'a').lookAhead * +'a' map { tuple -> tuple.b }
        val context = ParseContext("a", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals('a', result.value)
        assertEquals(0, result.start)
        assertEquals(1, result.end)
    }

    @Test
    fun multipleLookaheadParsersInSequence() {
        val parser = (+'a').lookAhead * (+'a').lookAhead * +'a'
        val result = parser.parseAllOrThrow("a")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('a', result.c)
    }

    @Test
    fun lookaheadParserWithRegex() {
        val parser = (+Regex("[0-9]+")).lookAhead * +Regex("[0-9]+") map { tuple -> tuple.b.value }
        val result = parser.parseAllOrThrow("123")
        assertEquals("123", result)
    }

    @Test
    fun lookaheadParserForKeywordLookahead() {
        val keyword = +"if"
        val notWordChar = +Regex("[^a-zA-Z0-9_]")
        val ifKeyword = keyword * notWordChar.lookAhead map { tuple -> tuple.a }

        val context1 = ParseContext("if ", useMemoization = true)
        val result1 = ifKeyword.parseOrNull(context1, 0)
        assertNotNull(result1)
        assertEquals("if", result1.value)
        assertEquals(2, result1.end)

        val context2 = ParseContext("ifx", useMemoization = true)
        val result2 = ifKeyword.parseOrNull(context2, 0)
        assertNull(result2)
    }

    @Test
    fun lookaheadParserVsCharSequence() {
        val parser = (+'a').lookAhead * +'a' * (+'b').lookAhead * +'b' * +'c'
        val result = parser.parseAllOrThrow("abc")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('b', result.c)
        assertEquals('b', result.d)
        assertEquals('c', result.e)
    }
}
