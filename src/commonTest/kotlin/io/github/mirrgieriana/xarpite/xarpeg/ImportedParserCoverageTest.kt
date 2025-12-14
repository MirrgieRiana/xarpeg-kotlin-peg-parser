package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.ExtraCharactersParseException
import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1
import io.github.mirrgieriana.xarpite.xarpeg.Tuple5
import io.github.mirrgieriana.xarpite.xarpeg.UnmatchedInputParseException
import io.github.mirrgieriana.xarpite.xarpeg.isNative
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.text
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.list
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.not
import io.github.mirrgieriana.xarpite.xarpeg.parsers.or
import io.github.mirrgieriana.xarpite.xarpeg.parsers.parser
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.rightAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.toParser
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unit
import io.github.mirrgieriana.xarpite.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertFailsWith

class ImportedParserCoverageTest {

    @Test
    fun stringParserCachesInstanceWhenNotFrozen() {
        if (isNative) return

        val first = +"cache"
        val second = +"cache"

        assertSame(first, second)
    }

    @Test
    fun charParserCachesInstanceWhenNotFrozen() {
        if (isNative) return

        val first = +'c'
        val second = +'c'

        assertSame(first, second)
    }

    @Test
    fun delegationParserInvokesGetterOnlyOnce() {
        var invoked = 0
        val delegating = parser {
            invoked++
            +'x'
        }

        assertEquals('x', delegating.parseAllOrThrow("x"))
        assertEquals('x', delegating.parseAllOrThrow("x"))
        assertEquals(1, invoked)
    }

    @Test
    fun parseContextCachesNullResults() {
        var counter = 0
        val countingParser = Parser<Int> { _, _ ->
            counter++
            null
        }
        val context = ParseContext("abc", useCache = true)

        assertNull(context.parseOrNull(countingParser, 0))
        assertNull(context.parseOrNull(countingParser, 0))
        assertEquals(1, counter)
    }

    @Test
    fun parseContextCacheSeparatesByStartIndex() {
        var counter = 0
        val countingParser = Parser<Int> { _, start ->
            counter++
            if (start >= 3) return@Parser null
            ParseResult(start, start, start + 1)
        }
        val context = ParseContext("abcd", useCache = true)

        assertEquals(0, context.parseOrNull(countingParser, 0)?.value)
        assertEquals(0, context.parseOrNull(countingParser, 0)?.value)
        assertEquals(1, context.parseOrNull(countingParser, 1)?.value)
        assertEquals(1, context.parseOrNull(countingParser, 1)?.value)
        assertEquals(2, counter)
    }

    @Test
    fun regexParserRespectsManualStartOffset() {
        val parser = +Regex("[ab]+")
        val context = ParseContext("zab", useCache = true)

        assertNull(parser.parseOrNull(context, 0))
        val result = parser.parseOrNull(context, 1)

        assertNotNull(result)
        assertEquals("ab", result.text(context))
    }

    @Test
    fun regexParserCalculatesEndUsingMatchRange() {
        val parser = +Regex("a+")
        val context = ParseContext("baaac", useCache = true)

        val result = parser.parseOrNull(context, 1)

        assertNotNull(result)
        assertEquals(4, result.end)
    }

    @Test
    fun mapExPreservesOffsetsAwayFromZero() {
        val parser = (+"cd") mapEx { ctx, result ->
            "${result.start}-${result.end}-${result.text(ctx)}"
        }
        val context = ParseContext("xxcdyy", useCache = true)

        val result = parser.parseOrNull(context, 2)

        assertNotNull(result)
        assertEquals("2-4-cd", result.value)
    }

    @Test
    fun notParserProducesZeroWidthResult() {
        val parser = !+'a'
        val context = ParseContext("b", useCache = true)

        val result = parser.parseOrNull(context, 0)

        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
        assertEquals(Tuple0, result.value)
    }

    @Test
    fun listParserRespectsMaxLimit() {
        val parser = (+'a').list(min = 1, max = 2)
        val context = ParseContext("aaab", useCache = true)

        val result = parser.parseOrNull(context, 0)

        assertNotNull(result)
        assertEquals(listOf('a', 'a'), result.value)
        assertEquals(2, result.end)
    }

    @Test
    fun leftAssociativeTrailingOperatorReportsExtraPosition() {
        val num = +Regex("\\d+") mapEx { ctx, result -> result.text(ctx).toInt() }
        val add = leftAssociative(num, -'+') { a, _, b -> a + b }

        val exception = assertFailsWith<ExtraCharactersParseException> { add.parseAllOrThrow("1+") }
        assertEquals(1, exception.position)
    }

    @Test
    fun rightAssociativeTrailingOperatorReportsExtraPosition() {
        val num = +Regex("\\d+") mapEx { ctx, result -> result.text(ctx).toInt() }
        val add = rightAssociative(num, -'+') { a, _, b -> a + b }

        val exception = assertFailsWith<ExtraCharactersParseException> { add.parseAllOrThrow("2+") }
        assertEquals(1, exception.position)
    }

    @Test
    fun parseAllOrThrowUnmatchedInputHasPosition() {
        val parser = +'a'

        val exception = assertFailsWith<UnmatchedInputParseException> { parser.parseAllOrThrow("") }

        assertEquals(0, exception.position)
    }

    @Test
    fun unitParserDoesNotAdvanceIndex() {
        val parser = unit("ok")
        val context = ParseContext("zzz", useCache = true)

        val result = parser.parseOrNull(context, 2)

        assertNotNull(result)
        assertEquals("ok", result.value)
        assertEquals(2, result.start)
        assertEquals(2, result.end)
    }

    @Test
    fun orParserPlusAppendsBranch() {
        val base = or(+'a', +'b')
        val parser = base + +'c'

        assertEquals('c', parser.parseAllOrThrow("c"))
        assertEquals('a', parser.parseAllOrThrow("a"))
    }

    @Test
    fun tupleCombinationTimes23ProducesTuple5() {
        val left = +'a' * +'b'
        val right = +'c' * +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAllOrThrow("abcde"))
    }

    @Test
    fun tupleCombinationTimes32ProducesTuple5() {
        val left = +'a' * +'b' * +'c'
        val right = +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAllOrThrow("abcde"))
    }

    @Test
    fun tupleCombinationTimes14ProducesTuple5() {
        val left: Parser<Tuple1<Char>> = +(+'a')
        val right = +'b' * +'c' * +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAllOrThrow("abcde"))
    }

    @Test
    fun tupleCombinationWithLeadingTuple0PassesThroughRight() {
        val left = -'x'
        val right: Parser<Tuple1<Char>> = +(+'y')
        val parser = left * right

        assertEquals(Tuple1('y'), parser.parseAllOrThrow("xy"))
    }

    @Test
    fun parseContextWithoutCacheInvokesParserEachTime() {
        var counter = 0
        val countingParser = Parser<Int> { _, start ->
            counter++
            ParseResult(start, start, start + 1)
        }
        val context = ParseContext("aaa", useCache = false)

        assertNotNull(context.parseOrNull(countingParser, 0))
        assertNotNull(context.parseOrNull(countingParser, 0))

        assertEquals(2, counter)
    }
}
