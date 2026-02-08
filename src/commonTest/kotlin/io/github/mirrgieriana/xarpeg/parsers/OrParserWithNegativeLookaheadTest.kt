package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Or parser combined with negative lookahead to analyze suggestion behavior.
 * 
 * This tests the original issue: `!"A" + +"B"` with input "C"
 */
class OrParserWithNegativeLookaheadTest {

    @Test
    fun orParserWithNegativeLookaheadAndInputC() {
        // Pattern: !"A" + +"B" = "match empty string if not followed by A, OR match B"
        // Input: "C"
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB
        
        val context = ParseContext("C", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        // !"A" should succeed (C is not A), matching empty string at position 0
        assertNotNull(result, "Parser should succeed matches empty string")
        assertEquals(0, result.start, "Start should be 0")
        assertEquals(0, result.end, "End should be 0 (empty match)")
        
        // No failures means no suggestions
        assertTrue(context.suggestedParsers.isEmpty(), "Should have no suggestions (parser succeeded)")
    }

    @Test
    fun orParserWithNegativeLookaheadAndInputA() {
        // Pattern: !"A" + +"B" = "match empty string if not followed by A, OR match B"
        // Input: "A"
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB
        
        val context = ParseContext("A", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        // !"A" fails (A IS A), +"B" fails (A is not B)
        assertNull(result, "Parser should fail")
        
        val suggestedNames = context.suggestedParsers.mapNotNull { it.name }
        // Only B should be suggested (not A, because it's in a negative lookahead)
        assertTrue(suggestedNames.contains("B"), "Should suggest 'B'")
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest 'A' (it's in negative lookahead)")
    }

    @Test
    fun orParserWithNegativeLookaheadAndInputB() {
        // Pattern: !"A" + +"B" = "match empty string if not followed by A, OR match B"
        // Input: "B"
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB
        
        val context = ParseContext("B", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        // First alternative succeeds (B is not A) OR +"B" succeeds
        // OrParser tries first alternative and succeeds
        assertNotNull(result, "Parser should succeed")
        assertEquals(0, result.start)
        assertEquals(0, result.end, "Should match empty string from first alternative")
        
        // No failures
        assertTrue(context.suggestedParsers.isEmpty(), "Should have no suggestions")
    }

    @Test
    fun orParserWithNegativeLookaheadAndEmptyInput() {
        // Pattern: !"A" + +"B"
        // Input: "" (empty)
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB
        
        val context = ParseContext("", useMemoization = true)
        val result = parser.parseOrNull(context, 0)
        
        // First alternative should succeed (empty string doesn't start with A)
        assertNotNull(result, "Parser should succeed")
        assertEquals(0, result.start)
        assertEquals(0, result.end)
        
        assertTrue(context.suggestedParsers.isEmpty(), "Should have no suggestions")
    }

    @Test
    fun compareSerialVsOrWithNegativeLookahead() {
        // Compare: !"A" * +"B" vs !"A" + +"B" with input "C"
        
        // Serial: !"A" * +"B" = "B not preceded by A"
        val parserA1 = +"A" named "A"
        val parserB1 = +"B" named "B"
        val serialParser = !parserA1 * parserB1
        
        val context1 = ParseContext("C", useMemoization = true)
        val result1 = serialParser.parseOrNull(context1, 0)
        
        assertNull(result1, "Serial parser should fail (first part succeeds but second part fails)")
        val suggestedNames1 = context1.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames1.contains("B"), "Serial: Should suggest B")
        assertTrue(!suggestedNames1.contains("A"), "Serial: Should NOT suggest A")
        
        // Or: !"A" + +"B" = "empty if not A, OR B"
        val parserA2 = +"A" named "A"
        val parserB2 = +"B" named "B"
        val orParser = !parserA2 + parserB2
        
        val context2 = ParseContext("C", useMemoization = true)
        val result2 = orParser.parseOrNull(context2, 0)
        
        assertNotNull(result2, "Or parser should succeed (first alternative matches)")
        assertTrue(context2.suggestedParsers.isEmpty(), "Or: Should have no suggestions")
    }
}
