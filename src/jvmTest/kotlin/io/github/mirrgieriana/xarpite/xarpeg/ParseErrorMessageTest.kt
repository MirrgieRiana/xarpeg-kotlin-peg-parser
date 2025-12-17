package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ParseErrorMessageTest {
    @Test
    fun errorMessageIncludesClosingParenthesis() {
        // Create a simple expression parser similar to the online parser
        val number = +Regex("[0-9]+")
        val primary = number + (-'(' * number * -')')
        val parser = primary * endOfInput
        
        // Try to parse an incomplete expression
        val result = parser.parseAll("(123")
        
        // Check that the error contains the closing parenthesis
        assertTrue(result.isFailure, "Parse should fail")
        val exception = result.exceptionOrNull() as? ParseException
        assertTrue(exception != null, "Should be a ParseException")
        
        // Check that closing paren is in suggested parsers
        val closingParenSuggested = exception.context.suggestedParsers.any { it.name == "\")\"" }
        assertTrue(closingParenSuggested, "Closing parenthesis should be in suggested parsers, got: ${exception.context.suggestedParsers.mapNotNull { it.name }}")
    }
}
