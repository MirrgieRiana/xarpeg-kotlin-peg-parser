package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.fail
import io.github.mirrgieriana.xarpeg.parsers.fixed
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.not
import io.github.mirrgieriana.xarpeg.parsers.oneOrMore
import io.github.mirrgieriana.xarpeg.parsers.optional
import io.github.mirrgieriana.xarpeg.parsers.or
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.rightAssociative
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParserAdditionalTest {

    @Test
    fun charParserFailsOnEmpty() {
        val parser = +'a'
        assertUnmatchedInput { parser.parseAll("").getOrThrow() }
    }

    @Test
    fun stringParserDetectsTrailingGarbage() {
        val parser = +"abc"
        assertExtraCharacters { parser.parseAll("abcc").getOrThrow() }
    }

    @Test
    fun regexParserAnchorsAtStart() {
        val parser = +Regex("[a-z]+") map { it.value }
        assertUnmatchedInput { parser.parseAll("1abc").getOrThrow() }
        assertEquals("abc", parser.parseAll("abc").getOrThrow())
    }

    @Test
    fun fixedParserRejectsConsumedInput() {
        val parser = fixed("ok")
        assertExtraCharacters { parser.parseAll("x").getOrThrow() }
    }

    @Test
    fun failParserRejectsAnyInput() {
        val parser = fail
        assertUnmatchedInput { parser.parseAll<Unit>("").getOrThrow() }
        assertUnmatchedInput { parser.parseAll<Unit>("anything").getOrThrow() }
    }

    @Test
    fun zeroOrMoreStopsBeforeMismatch() {
        val parser = (+'a').zeroOrMore
        val context = DefaultParseContext("aab")
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(listOf('a', 'a'), result.value)
        assertEquals(2, result.end)
    }

    @Test
    fun oneOrMoreFailsWithoutFirstMatch() {
        val parser = (+'b').oneOrMore
        assertUnmatchedInput { parser.parseAll("a").getOrThrow() }
    }

    @Test
    fun optionalDoesNotConsumeOnFailure() {
        val parser = (+'x').optional * +'y'
        assertEquals('y', parser.parseAll("y").getOrThrow().b)
    }

    @Test
    fun orParserTriesLaterBranches() {
        val parser = or(+'a', +'b', +'c')
        assertEquals('c', parser.parseAll("c").getOrThrow())
    }

    @Test
    fun notParserBlocksMatchingPrefix() {
        val parser = !+"ab" * +"cd"
        assertUnmatchedInput { parser.parseAll("ab").getOrThrow() }
        assertEquals("cd", parser.parseAll("cd").getOrThrow())
    }

    @Test
    fun ignoreParserConsumesValue() {
        val parser = -'q' * +'w'
        val result = parser.parseAll("qw").getOrThrow()
        assertEquals('w', result)
    }

    @Test
    fun referenceParserAllowsMutualRecursion() {
        val language = object {
            val number = +Regex("[0-9]+") map { it.value.toInt() }
            val term: Parser<Int> =
                number + (-'(' * ref { expr } * -')')
            val expr: Parser<Int> =
                leftAssociative(term, -'+') { a, _, b -> a + b }
        }
        assertEquals(6, language.expr.parseAll("1+2+3").getOrThrow())
        assertEquals(9, language.expr.parseAll("(4+5)").getOrThrow())
    }

    @Test
    fun leftAssociativeStopsAtGap() {
        val num = +Regex("[0-9]+") map { it.value.toInt() }
        val add = leftAssociative(num, -'+') { a, _, b -> a + b }
        assertExtraCharacters { add.parseAll("1+2x3").getOrThrow() }
    }

    @Test
    fun rightAssociativeConsumesChain() {
        val num = +Regex("[0-9]+") map { it.value }
        val pow = rightAssociative(num, -'^') { a, _, b -> "${a}^$b" }
        assertEquals("1^2^3", pow.parseAll("1^2^3").getOrThrow())
    }

    @Test
    fun mapPreservesRange() {
        val parser = +"hi" map { it.uppercase() }
        val context = DefaultParseContext("hi!")
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(2, result.end)
    }

    @Test
    fun mapThrowsArePropagated() {
        val parser = +'a' map { error("boom") }
        assertFails { parser.parseAll<Unit>("a").getOrThrow() }
    }

    @Test
    fun parseAllOrThrowWithoutCacheStillWorks() {
        val parser = (+'a').oneOrMore
        assertEquals(listOf('a', 'a'), parser.parseAll("aa") { DefaultParseContext(it).also { c -> c.useMemoization = false } }.getOrThrow())
    }

    @Test
    fun zeroOrMoreCanReturnEmptyList() {
        val parser = (+'z').zeroOrMore
        assertEquals(emptyList<Char>(), parser.parseAll("").getOrThrow())
    }

    @Test
    fun optionalReturnsTupleWithNull() {
        val parser = (+'k').optional
        val result = parser.parseAll("").getOrThrow()
        assertNull(result.a)
    }
}
