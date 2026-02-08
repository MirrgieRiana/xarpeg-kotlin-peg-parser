package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NegativeLookAheadParserTest {

    @Test
    fun negativeLookaheadParserMatchesWhenInnerParserFails() {
        val parser = (+'a').negativeLookAhead
        val context = ParseContext("bcd", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(0, result.end)
    }

    @Test
    fun negativeLookaheadParserFailsWhenInnerParserSucceeds() {
        val parser = (+'a').negativeLookAhead
        val context = ParseContext("abc", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNull(result)
    }

    @Test
    fun negativeLookaheadParserInSequence() {
        val parser = (+'a').negativeLookAhead * +'b'
        val result = parser.parseAllOrThrow("b")
        assertEquals('b', result)
    }

    @Test
    fun negativeLookaheadParserWithString() {
        val parser = (+"hello").negativeLookAhead * +"world"
        val result = parser.parseAllOrThrow("world")
        assertEquals("world", result)
    }

    @Test
    fun negativeLookaheadParserDoesNotConsumeInput() {
        val parser = (+'a').negativeLookAhead * +'b'
        val context = ParseContext("b", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        assertNotNull(result)
        assertEquals(0, result.start)
        assertEquals(1, result.end)
    }

    @Test
    fun negativeLookaheadParserUsingNotOperator() {
        val parser = !(+'a') * +'b'
        val result = parser.parseAllOrThrow("b")
        assertEquals('b', result)
    }

    @Test
    fun negativeLookaheadParserUsingNotProperty() {
        val parser = (+'a').not * +'b'
        val result = parser.parseAllOrThrow("b")
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
        
        val context = ParseContext("C", useMemoization = true)
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
        
        val context = ParseContext("D", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        assertNull(result, "Parser should fail on input 'D'")
        
        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames.contains("B"), "Should suggest 'B'")
        assertTrue(suggestedNames.contains("C"), "Should suggest 'C'")
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest 'A' (it's in a negative lookahead)")
    }

    @Test
    fun positiveLookaheadSuggestionsComparison() {
        // Compare with positive lookahead: (+parserA * parserB) with input "C"
        // Expected: suggest both A and B
        
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = (+parserA).lookAhead * parserB
        
        val context = ParseContext("C", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        assertNull(result, "Parser should fail on input 'C'")
        
        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        // With positive lookahead, both parsers should be suggested
        // because the lookahead parser failed
        assertTrue(suggestedNames.contains("A"), "Should suggest 'A'")
        assertTrue(suggestedNames.contains("B"), "Should suggest 'B'")
    }
}
