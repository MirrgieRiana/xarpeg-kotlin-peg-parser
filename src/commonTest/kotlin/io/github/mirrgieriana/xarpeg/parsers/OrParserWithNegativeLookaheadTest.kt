package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ExtraCharactersParseException
import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Or parser combined with negative lookahead to analyze suggestion behavior.
 *
 * This tests the original issue: `!"A" + +"B"` with input "C"
 */
class OrParserWithNegativeLookaheadTest {

    @Test
    fun orParserWithNegativeLookaheadAndInputC_parseAll() {
        // Pattern: !"A" + +"B" = "match empty string if not followed by A, OR match B"
        // Input: "C"
        // Expected: parseAll fails because !"A" matches empty string (consuming nothing),
        //           leaving "C" as extra characters
        //           Suggestions should be EMPTY (or性質上、Bは試されない)
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB

        val exception = assertFailsWith<ExtraCharactersParseException> {
            parser.parseAllOrThrow("C")
        }

        // The parser matched empty string at position 0, so extra characters start at 0
        assertEquals(0, exception.position, "Extra characters should start at position 0")

        // Due to Or parser behavior, only the first alternative (!"A") is tried
        // It succeeds, so +"B" is never attempted
        // Therefore suggestions should be EMPTY
        // Before fix: "A" would have been in suggestions (incorrect)
        // After fix: suggestions should be empty (correct, though not helpful due to Or nature)
        val suggestedNames = exception.context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames.isEmpty(), "Suggestions should be empty (Or parser stops at first success)")
        assertTrue(!suggestedNames.contains("A"), "Should NOT contain A (inside lookahead)")
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
        assertTrue(suggestedNames.contains("B"), "Should suggest B")
        assertTrue(!suggestedNames.contains("A"), "Should NOT suggest A (in negative lookahead)")
    }

    @Test
    fun orParserWithNegativeLookaheadAndInputB() {
        // Pattern: !"A" + +"B"
        // Input: "B"
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB

        // !"A" succeeds (B is not A), matches empty string
        // Due to Or parser, +"B" is not tried
        // parseAll will fail because only empty string was consumed
        val exception = assertFailsWith<ExtraCharactersParseException> {
            parser.parseAllOrThrow("B")
        }

        assertEquals(0, exception.position)
        val suggestedNames = exception.context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames.isEmpty(), "Suggestions should be empty")
    }

    @Test
    fun orParserWithNegativeLookaheadAndEmptyInput() {
        // Pattern: !"A" + +"B"
        // Input: "" (empty)
        val parserA = +"A" named "A"
        val parserB = +"B" named "B"
        val parser = !parserA + parserB

        // Should successfully match empty string (!"A" succeeds)
        val result = parser.parseAllOrThrow("")
        assertEquals(Tuple0, result)
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
        // With parseAll, this fails due to extra characters but suggestions are empty
        val parserA2 = +"A" named "A"
        val parserB2 = +"B" named "B"
        val orParser = !parserA2 + parserB2

        val exception = assertFailsWith<ExtraCharactersParseException> {
            orParser.parseAllOrThrow("C")
        }

        val suggestedNames2 = exception.context.suggestedParsers.mapNotNull { it.name }
        assertTrue(suggestedNames2.isEmpty(), "Or: Suggestions should be empty (first alternative succeeded)")
        assertTrue(!suggestedNames2.contains("A"), "Or: Should NOT contain A")
        assertTrue(!suggestedNames2.contains("B"), "Or: Should NOT contain B (never tried)")
    }
}

