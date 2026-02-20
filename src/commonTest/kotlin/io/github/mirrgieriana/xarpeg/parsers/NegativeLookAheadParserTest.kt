package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.DefaultParseContext
import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.parseAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NegativeLookAheadParserTest {

    @Test
    fun negativeLookaheadParserMatchesWhenInnerParserFails() {
        val parser = (+'a').negativeLookAhead
        val context = DefaultParseContext("bcd")
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun negativeLookaheadParserFailsWhenInnerParserSucceeds() {
        val parser = (+'a').negativeLookAhead
        val context = DefaultParseContext("abc")
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun negativeLookaheadParserInSequence() {
        val parser = (+'a').negativeLookAhead * +'b'
        val result = parser.parseAll("b").getOrThrow()
        assertEquals('b', result)
    }

    @Test
    fun negativeLookaheadParserWithString() {
        val parser = (+"hello").negativeLookAhead * +"world"
        val result = parser.parseAll("world").getOrThrow()
        assertEquals("world", result)
    }

    @Test
    fun negativeLookaheadParserDoesNotConsumeInput() {
        val parser = (+'a').negativeLookAhead * +'b'
        val context = DefaultParseContext("b")
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(1, result.end)
    }

    @Test
    fun negativeLookaheadParserUsingNotOperator() {
        val parser = !(+'a') * +'b'
        val result = parser.parseAll("b").getOrThrow()
        assertEquals('b', result)
    }

    @Test
    fun negativeLookaheadParserUsingNotProperty() {
        val parser = (+'a').not * +'b'
        val result = parser.parseAll("b").getOrThrow()
        assertEquals('b', result)
    }

    @Test
    fun negativeLookaheadSuggestionsTest() {
        // Test case: !"A" * +"B" with input "C"
        // Expected: fail but suggest only B
        // Problem: currently suggests both A and B

        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA * parserB

        val context = DefaultParseContext("C")
        val result = parser.parseOrNull(context, 0)

        assertNull(result, "Parser should fail on input 'C'")
        assertEquals(0, context.errorPosition, "Error position should be 0")

        // Only "B" should be suggested because:
        // - !"A" succeeds (because input is not "A"), so it doesn't fail and shouldn't be suggested
        // - +"B" fails (because input is not "B"), so it should be suggested
        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames.contains("B"), "Should suggest 'B'")
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest 'A' (it's in a negative lookahead)")
    }

    @Test
    fun negativeLookaheadSuggestionsWithMultipleAlternatives() {
        // Test: (!parserA + parserB) + parserC with input "D"
        // Expected: suggest both B and C, but not A

        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parserC = +"C" named "C"
        val parser = (!parserA * parserB) + parserC

        val context = DefaultParseContext("D")
        val result = parser.parseOrNull(context, 0)

        assertNull(result, "Parser should fail on input 'D'")

        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames.contains("B"), "Should suggest 'B'")
        assertTrue(suggestedNames.contains("C"), "Should suggest 'C'")
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest 'A' (it's in a negative lookahead)")
    }

    @Test
    fun positiveLookaheadSuggestionsComparison() {
        // Compare with positive lookahead: (+parserA).lookAhead * parserB with input "C"
        //
        // Note: With the current implementation, lookahead parsers (both positive and negative)
        // do not add their internal parsers to suggestions. This is because lookahead doesn't
        // consume input - the actual consumption happens in subsequent parsers.
        //
        // In this case: (+parserA).lookAhead * parserB with input "C"
        // - The lookahead for "A" fails (C is not A)
        // - The whole parser fails
        // - Only the lookahead itself might be in suggestions, but not the internal "A" parser

        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = (+parserA).lookAhead * parserB

        val context = DefaultParseContext("C")
        val result = parser.parseOrNull(context, 0)

        assertNull(result, "Parser should fail on input 'C'")

        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        // The lookahead parser failed, but its internal parser "A" is not in suggestions
        // because lookahead doesn't consume input
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest 'A' (it's in a lookahead)")
    }
}
