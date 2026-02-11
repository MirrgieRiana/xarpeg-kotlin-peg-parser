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

/**
 * パーサーの追加的な動作をテストする
 */
class ParserAdditionalTest {

    @Test
    fun charParserFailsOnEmpty() {
        val parser = +'a'
        assertUnmatchedInput { parser.parseAllOrThrow("") }
    }

    @Test
    fun stringParserDetectsTrailingGarbage() {
        val parser = +"abc"
        assertExtraCharacters { parser.parseAllOrThrow("abcc") }
    }

    @Test
    fun regexParserAnchorsAtStart() {
        val parser = +Regex("[a-z]+") map { it.value }
        assertUnmatchedInput { parser.parseAllOrThrow("1abc") }
        assertEquals("abc", parser.parseAllOrThrow("abc"))
    }

    @Test
    fun fixedParserRejectsConsumedInput() {
        val parser = fixed("ok")
        assertExtraCharacters { parser.parseAllOrThrow("x") }
    }

    @Test
    fun failParserRejectsAnyInput() {
        val parser = fail
        assertUnmatchedInput { parser.parseAllOrThrow("") }
        assertUnmatchedInput { parser.parseAllOrThrow("anything") }
    }

    @Test
    fun zeroOrMoreStopsBeforeMismatch() {
        val parser = (+'a').zeroOrMore
        val context = ParseContext("aab", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(listOf('a', 'a'), result.value)
        assertEquals(2, result.end)
    }

    @Test
    fun oneOrMoreFailsWithoutFirstMatch() {
        val parser = (+'b').oneOrMore
        assertUnmatchedInput { parser.parseAllOrThrow("a") }
    }

    @Test
    fun optionalDoesNotConsumeOnFailure() {
        val parser = (+'x').optional * +'y'
        assertEquals('y', parser.parseAllOrThrow("y").b)
    }

    @Test
    fun orParserTriesLaterBranches() {
        val parser = or(+'a', +'b', +'c')
        assertEquals('c', parser.parseAllOrThrow("c"))
    }

    @Test
    fun notParserBlocksMatchingPrefix() {
        val parser = !+"ab" * +"cd"
        assertUnmatchedInput { parser.parseAllOrThrow("ab") }
        assertEquals("cd", parser.parseAllOrThrow("cd"))
    }

    @Test
    fun ignoreParserConsumesValue() {
        val parser = -'q' * +'w'
        val result = parser.parseAllOrThrow("qw")
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
        assertEquals(6, language.expr.parseAllOrThrow("1+2+3"))
        assertEquals(9, language.expr.parseAllOrThrow("(4+5)"))
    }

    @Test
    fun leftAssociativeStopsAtGap() {
        val num = +Regex("[0-9]+") map { it.value.toInt() }
        val add = leftAssociative(num, -'+') { a, _, b -> a + b }
        assertExtraCharacters { add.parseAllOrThrow("1+2x3") }
    }

    @Test
    fun rightAssociativeConsumesChain() {
        val num = +Regex("[0-9]+") map { it.value }
        val pow = rightAssociative(num, -'^') { a, _, b -> "${a}^$b" }
        assertEquals("1^2^3", pow.parseAllOrThrow("1^2^3"))
    }

    @Test
    fun mapPreservesRange() {
        val parser = +"hi" map { it.uppercase() }
        val context = ParseContext("hi!", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(2, result.end)
    }

    @Test
    fun mapThrowsArePropagated() {
        val parser = +'a' map { error("boom") }
        assertFails { parser.parseAllOrThrow("a") }
    }

    @Test
    fun parseAllOrThrowWithoutCacheStillWorks() {
        val parser = (+'a').oneOrMore
        assertEquals(listOf('a', 'a'), parser.parseAllOrThrow("aa", useMemoization = false))
    }

    @Test
    fun zeroOrMoreCanReturnEmptyList() {
        val parser = (+'z').zeroOrMore
        assertEquals(emptyList<Char>(), parser.parseAllOrThrow(""))
    }

    @Test
    fun optionalReturnsTupleWithNull() {
        val parser = (+'k').optional
        val result = parser.parseAllOrThrow("")
        assertNull(result.a)
    }
}
