import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mirrg.xarpite.parser.NumberParser
import mirrg.xarpite.parser.ParseContext
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.leftAssociative
import mirrg.xarpite.parser.parsers.map
import mirrg.xarpite.parser.parsers.not
import mirrg.xarpite.parser.parsers.nothing
import mirrg.xarpite.parser.parsers.oneOrMore
import mirrg.xarpite.parser.parsers.optional
import mirrg.xarpite.parser.parsers.or
import mirrg.xarpite.parser.parsers.parser
import mirrg.xarpite.parser.parsers.plus
import mirrg.xarpite.parser.parsers.rightAssociative
import mirrg.xarpite.parser.parsers.times
import mirrg.xarpite.parser.parsers.unaryMinus
import mirrg.xarpite.parser.parsers.unaryPlus
import mirrg.xarpite.parser.parsers.unit
import mirrg.xarpite.parser.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ParserAdditionalTest {

    @Test
    fun charParserFailsOnEmpty() = runTest {
        val parser = +'a'
        assertUnmatchedInput { parser.parseAllOrThrow("") }
    }

    @Test
    fun stringParserDetectsTrailingGarbage() = runTest {
        val parser = +"abc"
        assertExtraCharacters { parser.parseAllOrThrow("abcc") }
    }

    @Test
    fun regexParserAnchorsAtStart() = runTest {
        val parser = +Regex("[a-z]+")
        assertUnmatchedInput { parser.parseAllOrThrow("1abc") }
        assertEquals("abc", parser.parseAllOrThrow("abc"))
    }

    @Test
    fun unitParserRejectsConsumedInput() = runTest {
        val parser = unit("ok")
        assertExtraCharacters { parser.parseAllOrThrow("x") }
    }

    @Test
    fun nothingParserRejectsAnyInput() = runTest {
        val parser = nothing
        assertUnmatchedInput { parser.parseAllOrThrow("") }
        assertUnmatchedInput { parser.parseAllOrThrow("anything") }
    }

    @Test
    fun zeroOrMoreStopsBeforeMismatch() = runTest {
        val parser = (+'a').zeroOrMore
        val context = ParseContext("aab", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(listOf('a', 'a'), result.value)
        assertEquals(2, result.end)
    }

    @Test
    fun oneOrMoreFailsWithoutFirstMatch() = runTest {
        val parser = (+'b').oneOrMore
        assertUnmatchedInput { parser.parseAllOrThrow("a") }
    }

    @Test
    fun optionalDoesNotConsumeOnFailure() = runTest {
        val parser = (+'x').optional * +'y'
        assertEquals('y', parser.parseAllOrThrow("y").b)
    }

    @Test
    fun orParserTriesLaterBranches() = runTest {
        val parser = or(+'a', +'b', +'c')
        assertEquals('c', parser.parseAllOrThrow("c"))
    }

    @Test
    fun notParserBlocksMatchingPrefix() = runTest {
        val parser = !+"ab" * +"cd"
        assertUnmatchedInput { parser.parseAllOrThrow("ab") }
        assertEquals("cd", parser.parseAllOrThrow("cd"))
    }

    @Test
    fun ignoreParserConsumesValue() = runTest {
        val parser = -'q' * +'w'
        val result = parser.parseAllOrThrow("qw")
        assertEquals('w', result.b)
    }

    @Test
    fun delegationParserAllowsMutualRecursion() = runTest {
        val language = object {
            val term = +Regex("[0-9]+") + ( -'(' * parser { expr } * -')' )
            val expr: mirrg.xarpite.parser.Parser<Int> by lazy {
                leftAssociative(term, -'+') { a, _, b -> a + b }
            }
        }
        assertEquals(6, language.expr.parseAllOrThrow("1+2+3"))
        assertEquals(9, language.expr.parseAllOrThrow("(4+5)"))
    }

    @Test
    fun leftAssociativeStopsAtGap() = runTest {
        val num = +Regex("[0-9]+") map { it.value.toInt() }
        val add = leftAssociative(num, -'+') { a, _, b -> a + b }
        assertExtraCharacters { add.parseAllOrThrow("1+2x3") }
    }

    @Test
    fun rightAssociativeConsumesChain() = runTest {
        val num = +Regex("[0-9]+")
        val pow = rightAssociative(num, -'^') { a, _, b -> "${a}^$b" }
        assertEquals("1^2^3", pow.parseAllOrThrow("1^2^3"))
    }

    @Test
    fun mapPreservesRange() = runTest {
        val parser = (+"hi") map { it.uppercase() }
        val context = ParseContext("hi!", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(2, result.end)
    }

    @Test
    fun mapThrowsArePropagated() = runTest {
        val parser = (+'a') map { error("boom") }
        assertFails { parser.parseAllOrThrow("a") }
    }

    @Test
    fun numberParserRespectsStartOffset() = runTest {
        val context = ParseContext("xx42yy", useCache = true)
        val result = NumberParser.parseOrNull(context, 2)
        assertNotNull(result)
        assertEquals(42, result.value)
        assertEquals(4, result.end)
    }

    @Test
    fun parseAllOrThrowWithoutCacheStillWorks() = runTest {
        val parser = (+'a').oneOrMore
        assertEquals(listOf('a', 'a'), parser.parseAllOrThrow("aa", useCache = false))
    }

    @Test
    fun zeroOrMoreCanReturnEmptyList() = runTest {
        val parser = (+'z').zeroOrMore
        assertEquals(emptyList<Char>(), parser.parseAllOrThrow(""))
    }

    @Test
    fun optionalReturnsTupleWithNull() = runTest {
        val parser = (+'k').optional
        val result = parser.parseAllOrThrow("")
        assertNull(result.a)
    }
}
