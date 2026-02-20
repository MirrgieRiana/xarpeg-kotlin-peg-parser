package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.fixed
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.list
import io.github.mirrgieriana.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpeg.parsers.not
import io.github.mirrgieriana.xarpeg.parsers.or
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.rightAssociative
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

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
    fun referenceParserInvokesGetterOnlyOnce() {
        var invoked = 0
        val delegating = ref {
            invoked++
            +'x'
        }

        assertEquals('x', delegating.parseAll("x").getOrThrow())
        assertEquals('x', delegating.parseAll("x").getOrThrow())
        assertEquals(1, invoked)
    }

    @Test
    fun parseContextCachesNullResults() {
        var counter = 0
        val countingParser = Parser<Int> { _, _ ->
            counter++
            null
        }
        val context = DefaultParseContext("abc")

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
        val context = DefaultParseContext("abcd")

        assertEquals(0, context.parseOrNull(countingParser, 0)?.value)
        assertEquals(0, context.parseOrNull(countingParser, 0)?.value)
        assertEquals(1, context.parseOrNull(countingParser, 1)?.value)
        assertEquals(1, context.parseOrNull(countingParser, 1)?.value)
        assertEquals(2, counter)
    }

    @Test
    fun regexParserRespectsManualStartOffset() {
        val parser = +Regex("[ab]+")
        val context = DefaultParseContext("zab")

        assertNull(parser.parseOrNull(context, 0))
        val result = parser.parseOrNull(context, 1)

        assertNotNull(result)
        assertEquals("ab", result.text(context))
    }

    @Test
    fun regexParserCalculatesEndUsingMatchRange() {
        val parser = +Regex("a+")
        val context = DefaultParseContext("baaac")

        val result = parser.parseOrNull(context, 1)

        assertNotNull(result)
        assertEquals(4, result.end)
    }

    @Test
    fun mapExPreservesOffsetsAwayFromZero() {
        val parser = (+"cd") mapEx { ctx, result ->
            "${result.start}-${result.end}-${result.text(ctx)}"
        }
        val context = DefaultParseContext("xxcdyy")

        val result = parser.parseOrNull(context, 2)

        assertNotNull(result)
        assertEquals("2-4-cd", result.value)
    }

    @Test
    fun notParserProducesZeroWidthResult() {
        val parser = !+'a'
        val context = DefaultParseContext("b")

        val result = parser.parseOrNull(context, 0)

        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
        assertEquals(Tuple0, result.value)
    }

    @Test
    fun listParserRespectsMaxLimit() {
        val parser = (+'a').list(min = 1, max = 2)
        val context = DefaultParseContext("aaab")

        val result = parser.parseOrNull(context, 0)

        assertNotNull(result)
        assertEquals(listOf('a', 'a'), result.value)
        assertEquals(2, result.end)
    }

    @Test
    fun leftAssociativeTrailingOperatorReportsExtraPosition() {
        val num = +Regex("\\d+") mapEx { ctx, result -> result.text(ctx).toInt() }
        val add = leftAssociative(num, -'+') { a, _, b -> a + b }

        val exception = assertFailsWith<ParseException> { add.parseAll("1+").getOrThrow() }
        // errorPosition points to position 2 (after "+"), where the parser attempted to find another number
        assertEquals(2, exception.context.errorPosition ?: 0)
    }

    @Test
    fun rightAssociativeTrailingOperatorReportsExtraPosition() {
        val num = +Regex("\\d+") mapEx { ctx, result -> result.text(ctx).toInt() }
        val add = rightAssociative(num, -'+') { a, _, b -> a + b }

        val exception = assertFailsWith<ParseException> { add.parseAll("2+").getOrThrow() }
        // errorPosition points to position 2 (after "+"), where the parser attempted to find another number
        assertEquals(2, exception.context.errorPosition ?: 0)
    }

    @Test
    fun parseAllOrThrowUnmatchedInputHasPosition() {
        val parser = +'a'

        val exception = assertFailsWith<ParseException> { parser.parseAll("").getOrThrow() }

        assertEquals(0, exception.context.errorPosition ?: 0)
    }

    @Test
    fun fixedParserDoesNotAdvanceIndex() {
        val parser = fixed("ok")
        val context = DefaultParseContext("zzz")

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

        assertEquals('c', parser.parseAll("c").getOrThrow())
        assertEquals('a', parser.parseAll("a").getOrThrow())
    }

    @Test
    fun tupleCombinationTimes23ProducesTuple5() {
        val left = +'a' * +'b'
        val right = +'c' * +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAll("abcde").getOrThrow())
    }

    @Test
    fun tupleCombinationTimes32ProducesTuple5() {
        val left = +'a' * +'b' * +'c'
        val right = +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAll("abcde").getOrThrow())
    }

    @Test
    fun tupleCombinationTimes14ProducesTuple5() {
        val left: Parser<Tuple1<Char>> = +(+'a')
        val right = +'b' * +'c' * +'d' * +'e'
        val parser = left * right

        assertEquals(Tuple5('a', 'b', 'c', 'd', 'e'), parser.parseAll("abcde").getOrThrow())
    }

    @Test
    fun tupleCombinationWithLeadingTuple0PassesThroughRight() {
        val left = -'x'
        val right: Parser<Tuple1<Char>> = +(+'y')
        val parser = left * right

        assertEquals(Tuple1('y'), parser.parseAll("xy").getOrThrow())
    }

    @Test
    fun parseContextWithoutCacheInvokesParserEachTime() {
        var counter = 0
        val countingParser = Parser<Int> { _, start ->
            counter++
            ParseResult(start, start, start + 1)
        }
        val context = DefaultParseContext("aaa").also { it.useMemoization = false }

        assertNotNull(context.parseOrNull(countingParser, 0))
        assertNotNull(context.parseOrNull(countingParser, 0))

        assertEquals(2, counter)
    }
}
