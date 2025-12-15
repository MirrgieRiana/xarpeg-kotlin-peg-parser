package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NamedParserTest {

    @Test
    fun namedParserHasName() {
        val parser = (+'a') named "letter_a"
        assertEquals("letter_a", parser.name)
    }

    @Test
    fun namedParserParsesSuccessfully() {
        val parser = (+'a') named "letter_a"
        val result = parser.parseAllOrThrow("a")
        assertEquals('a', result)
    }

    @Test
    fun namedParserFailsOnMismatch() {
        val parser = (+'a') named "letter_a"
        val context = ParseContext("b", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun namedParserInSequence() {
        val parserA = (+'a') named "letter_a"
        val parserB = (+'b') named "letter_b"
        val combined = parserA * parserB
        val result = combined.parseAllOrThrow("ab")
        assertEquals('a', result.a)
        assertEquals('b', result.b)
    }

    @Test
    fun namedParserInChoice() {
        val parserA = (+'a') named "letter_a"
        val parserB = (+'b') named "letter_b"
        val combined = parserA + parserB
        val result1 = combined.parseAllOrThrow("a")
        assertEquals('a', result1)
        val result2 = combined.parseAllOrThrow("b")
        assertEquals('b', result2)
    }

    @Test
    fun namedParserWithMap() {
        val parser = (+'a') named "letter_a" map { it.uppercaseChar() }
        val result = parser.parseAllOrThrow("a")
        assertEquals('A', result)
    }

    @Test
    fun namedParserWithString() {
        val parser = (+"hello") named "greeting"
        val result = parser.parseAllOrThrow("hello")
        assertEquals("hello", result)
    }

    @Test
    fun namedParserWithRegex() {
        val parser = (+Regex("[0-9]+")) named "number" map { it.value.toInt() }
        val result = parser.parseAllOrThrow("123")
        assertEquals(123, result)
    }

    @Test
    fun namedParserWithOptional() {
        val parser = ((+'a') named "letter_a").optional
        val result1 = parser.parseAllOrThrow("a")
        assertNotNull(result1.a)
        assertEquals('a', result1.a)
        
        val result2 = parser.parseAllOrThrow("")
        assertNull(result2.a)
    }

    @Test
    fun namedParserWithRepetition() {
        val parser = ((+'a') named "letter_a").oneOrMore
        val result = parser.parseAllOrThrow("aaa")
        assertEquals(listOf('a', 'a', 'a'), result)
    }

    @Test
    fun multipleNamedParsersInSequence() {
        val digit = (+Regex("[0-9]")) named "digit" map { it.value.toInt() }
        val operator = (+'+'  + +'-') named "operator" map { it }
        val expression = digit * operator * digit
        
        val result = expression.parseAllOrThrow("3+5")
        assertEquals(3, result.a)
        assertEquals('+', result.b)
        assertEquals(5, result.c)
    }

    @Test
    fun namedParserWithLookAhead() {
        val parser = ((+'a') named "letter_a").lookAhead
        val context = ParseContext("a", useCache = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals('a', result.value)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun namedParserWithNegativeLookAhead() {
        val parser = ((+'a') named "letter_a").negativeLookAhead
        val context1 = ParseContext("b", useCache = true)
        val result1 = parser.parseOrNull(context1, 0)
        assertNotNull(result1)
        
        val context2 = ParseContext("a", useCache = true)
        val result2 = parser.parseOrNull(context2, 0)
        assertNull(result2)
    }

    @Test
    fun namedParserInComplexGrammar() {
        val number = (+Regex("[0-9]+")) named "number" map { it.value.toInt() }
        val lparen = -'('
        val rparen = -')'
        val expr = lparen * number * rparen
        
        val result = expr.parseAllOrThrow("(42)")
        assertEquals(42, result)
    }

    @Test
    fun namedParserWithRecursion() {
        val grammar = object {
            val digit = (+Regex("[0-9]")) named "digit" map { it.value.toInt() }
            val lparen = -'('
            val rparen = -')'
            val expr: Parser<Int> = (digit + (lparen * ref { expr } * rparen)) named "expression"
        }
        
        val result1 = grammar.expr.parseAllOrThrow("5")
        assertEquals(5, result1)
        
        val result2 = grammar.expr.parseAllOrThrow("((3))")
        assertEquals(3, result2)
    }

    @Test
    fun namedParserErrorTracking() {
        // Test that named parsers are tracked correctly for error reporting
        val letter = (+Regex("[a-z]")) named "letter" map { it.value }
        val digit = (+Regex("[0-9]")) named "digit" map { it.value }
        val identifier = letter * (letter + digit).zeroOrMore
        
        val context = ParseContext("1abc", useCache = true)
        val result = identifier.parseOrNull(context, 0)
        
        assertNull(result)
        assertEquals(0, context.errorPosition)
        // Should suggest "letter" at position 0
        assertTrue(context.suggestedParsers.any { it.name == "letter" })
    }

    @Test
    fun namedParserCaching() {
        val parser = (+Regex("[a-z]+")) named "word" map { it.value }
        
        // Test with cache enabled
        val result1 = parser.parseAllOrThrow("hello", useCache = true)
        assertEquals("hello", result1)
        
        // Test with cache disabled
        val result2 = parser.parseAllOrThrow("world", useCache = false)
        assertEquals("world", result2)
    }

    @Test
    fun namedParserToString() {
        val parser = (+'a') named "letter_a"
        // The parser should still have some string representation
        assertNotNull(parser.toString())
    }

    @Test
    fun chainingNamedParsers() {
        // Test that naming can be applied at different levels
        val base = +'a'
        val named1 = base named "first_name"
        val named2 = named1 named "second_name"
        
        assertEquals("second_name", named2.name)
        val result = named2.parseAllOrThrow("a")
        assertEquals('a', result)
    }
}
