package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.assertExtraCharacters
import io.github.mirrgieriana.xarpeg.assertUnmatchedInput
import io.github.mirrgieriana.xarpeg.parseAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SerialParserTest {

    @Test
    fun serialParserMatchesAllParsersInOrder() {
        val parser = serial(+'a', +'b', +'c')
        val result = parser.parseAll("abc").getOrThrow()
        assertEquals(listOf('a', 'b', 'c'), result)
    }

    @Test
    fun serialParserFailsIfFirstParserFails() {
        val parser = serial(+'a', +'b', +'c')
        assertUnmatchedInput { parser.parseAll("xbc").getOrThrow() }
    }

    @Test
    fun serialParserFailsIfMiddleParserFails() {
        val parser = serial(+'a', +'b', +'c')
        assertUnmatchedInput { parser.parseAll("axc").getOrThrow() }
    }

    @Test
    fun serialParserFailsIfLastParserFails() {
        val parser = serial(+'a', +'b', +'c')
        assertUnmatchedInput { parser.parseAll("abx").getOrThrow() }
    }

    @Test
    fun serialParserWorksWithStringParsers() {
        val parser = serial(+"hello", +" ", +"world")
        val result = parser.parseAll("hello world").getOrThrow()
        assertEquals(listOf("hello", " ", "world"), result)
    }

    @Test
    fun serialParserWorksWithMixedParsers() {
        val word = +Regex("[a-z]+") map { it.value }
        val space = +" "
        val parser = serial(word, space, word, space, word)
        val result = parser.parseAll("one two three").getOrThrow()
        assertEquals(listOf("one", " ", "two", " ", "three"), result)
    }

    @Test
    fun serialParserHandlesLargeNumberOfParsers() {
        // Test that it works beyond tuple parser limit (16)
        val parsers = (1..20).map { i -> +('a' + i - 1) }
        val parser = serial(*parsers.toTypedArray())
        val input = "abcdefghijklmnopqrst"
        val result = parser.parseAll(input).getOrThrow()
        assertEquals(input.toList(), result)
    }

    @Test
    fun serialParserWithSingleParser() {
        val parser = serial(+'x')
        val result = parser.parseAll("x").getOrThrow()
        assertEquals(listOf('x'), result)
    }

    @Test
    fun serialParserWithTwoParsers() {
        val parser = serial(+'x', +'y')
        val result = parser.parseAll("xy").getOrThrow()
        assertEquals(listOf('x', 'y'), result)
    }

    @Test
    fun serialParserStopsAtCorrectPosition() {
        val parser = serial(+'a', +'b')
        val context = ParseContext("abc", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(listOf('a', 'b'), result.value)
        assertEquals(0, result.start)
        assertEquals(2, result.end)
    }

    @Test
    fun serialParserReturnsNullOnFailure() {
        val parser = serial(+'a', +'b', +'c')
        val context = ParseContext("abd", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun serialParserCanBeUsedInComplexGrammar() {
        // Example: parsing a natural language phrase with optional parts
        val article = +"the" + +"a"
        val adjective = +"quick" + +"lazy"
        val noun = +"fox" + +"dog"

        val phrase = serial(article, +" ", adjective, +" ", noun)

        val result1 = phrase.parseAll("the quick fox").getOrThrow()
        assertEquals(listOf("the", " ", "quick", " ", "fox"), result1)

        val result2 = phrase.parseAll("a lazy dog").getOrThrow()
        assertEquals(listOf("a", " ", "lazy", " ", "dog"), result2)
    }

    @Test
    fun serialParserWorksWithMappedParsers() {
        val digit = +Regex("[0-9]") map { it.value.toInt() }
        val parser = serial(digit, digit, digit)
        val result = parser.parseAll("123").getOrThrow()
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun serialParserPreservesOrder() {
        val parser = serial(+"first", +"second", +"third")
        val result = parser.parseAll("firstsecondthird").getOrThrow()
        assertEquals(listOf("first", "second", "third"), result)
        assertEquals("first", result[0])
        assertEquals("second", result[1])
        assertEquals("third", result[2])
    }

    @Test
    fun serialParserDetectsExtraCharacters() {
        val parser = serial(+'a', +'b')
        assertExtraCharacters { parser.parseAll("abc").getOrThrow() }
    }
}
