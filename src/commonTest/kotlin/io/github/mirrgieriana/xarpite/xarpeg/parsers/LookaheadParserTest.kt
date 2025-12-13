package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.or
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LookaheadParserTest {

    @Test
    fun lookaheadParserMatchesWithoutConsuming() {
        val parser = (+'a').lookahead()
        val context = ParseContext("abc", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals('a', result.value)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun lookaheadParserFailsWhenInnerParserFails() {
        val parser = (+'a').lookahead()
        val context = ParseContext("bcd", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun lookaheadParserInSequence() {
        val parser = (+'a').lookahead() * +'a' * +'b'
        val result = parser.parseAllOrThrow("ab")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('b', result.c)
    }

    @Test
    fun lookaheadParserWithString() {
        val parser = (+"hello").lookahead() * +"hello" * +"world"
        val result = parser.parseAllOrThrow("helloworld")
        assertEquals("hello", result.a)
        assertEquals("hello", result.b)
        assertEquals("world", result.c)
    }

    @Test
    fun lookaheadParserDoesNotConsumeInput() {
        val parser = (+'a').lookahead() * +'a' map { tuple -> tuple.b }
        val context = ParseContext("a", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals('a', result.value)
        assertEquals(0, result.start)
        assertEquals(1, result.end)
    }

    @Test
    fun multipleLookaheadParsersInSequence() {
        val parser = (+'a').lookahead() * (+'a').lookahead() * +'a'
        val result = parser.parseAllOrThrow("a")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('a', result.c)
    }

    @Test
    fun lookaheadParserWithRegex() {
        val parser = (+Regex("[0-9]+")).lookahead() * +Regex("[0-9]+") map { tuple -> tuple.b.value }
        val result = parser.parseAllOrThrow("123")
        assertEquals("123", result)
    }

    @Test
    fun lookaheadParserForKeywordLookahead() {
        val keyword = +"if"
        val notWordChar = +Regex("[^a-zA-Z0-9_]")
        val ifKeyword = keyword * notWordChar.lookahead() map { tuple -> tuple.a }
        
        val context1 = ParseContext("if ", useCache = true)
        val result1 = ifKeyword.parseOrNull(context1, 0)
        assertNotNull(result1)
        assertEquals("if", result1.value)
        assertEquals(2, result1.end)
        
        val context2 = ParseContext("ifx", useCache = true)
        val result2 = ifKeyword.parseOrNull(context2, 0)
        assertNull(result2)
    }

    @Test
    fun lookaheadParserVsCharSequence() {
        val parser = (+'a').lookahead() * +'a' * (+'b').lookahead() * +'b' * +'c'
        val result = parser.parseAllOrThrow("abc")
        assertEquals('a', result.a)
        assertEquals('a', result.b)
        assertEquals('b', result.c)
        assertEquals('b', result.d)
        assertEquals('c', result.e)
    }
}
