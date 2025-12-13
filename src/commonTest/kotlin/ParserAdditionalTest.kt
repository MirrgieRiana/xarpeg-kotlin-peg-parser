import assertExtraCharacters
import assertUnmatchedInput
import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun unitParserRejectsConsumedInput() {
        val parser = unit("ok")
        assertExtraCharacters { parser.parseAllOrThrow("x") }
    }

    @Test
    fun nothingParserRejectsAnyInput() {
        val parser = nothing
        assertUnmatchedInput { parser.parseAllOrThrow("") }
        assertUnmatchedInput { parser.parseAllOrThrow("anything") }
    }

    @Test
    fun zeroOrMoreStopsBeforeMismatch() {
        val parser = (+'a').zeroOrMore
        val context = ParseContext("aab", useCache = true)
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
    fun delegationParserAllowsMutualRecursion() {
        val language = object {
            val number = +Regex("[0-9]+") map { it.value.toInt() }
            val term: Parser<Int> by lazy {
                number + (-'(' * parser { expr } * -')')
            }
            val expr: Parser<Int> by lazy {
                leftAssociative(term, -'+') { a, _, b -> a + b }
            }
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
        val parser = (+"hi") map { it.uppercase() }
        val context = ParseContext("hi!", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(2, result.end)
    }

    @Test
    fun mapThrowsArePropagated() {
        val parser = (+'a') map { error("boom") }
        assertFails { parser.parseAllOrThrow("a") }
    }

    @Test
    fun parseAllOrThrowWithoutCacheStillWorks() {
        val parser = (+'a').oneOrMore
        assertEquals(listOf('a', 'a'), parser.parseAllOrThrow("aa", useCache = false))
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
