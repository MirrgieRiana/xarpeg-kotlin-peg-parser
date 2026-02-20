package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests demonstrating how to use error context information from ParseContext.
 *
 * When parsing fails, the ParseContext provides:
 * - errorPosition: The position in the input where parsing failed (the furthest position reached)
 * - suggestedParsers: A set of parsers that failed at the errorPosition
 *
 * These features are especially useful for providing helpful error messages to users.
 */
class ErrorContextTest {

    @Test
    fun errorPositionTracksFailurePoint() {
        // When parsing fails, errorPosition tells you where the parser was attempted
        val parser = +"hello"
        val context = DefaultParseContext("help")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        // Parser was attempted at position 0 and failed
        assertEquals(0, context.errorPosition)
    }

    @Test
    fun errorPositionWithSimpleChoice() {
        // With choice operators, errorPosition shows where parsing was attempted
        val parser = (+"hello" + +"world")
        val context = DefaultParseContext("help")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        // Both alternatives attempted at position 0 and failed
        assertEquals(0, context.errorPosition)
    }

    @Test
    fun errorPositionWithSequence() {
        // In sequences, errorPosition shows the furthest position reached
        val hello = +"hello"
        val space = +' '
        val world = +"world"
        val parser = hello * space * world

        val context = DefaultParseContext("hello world!")
        // Note: This will succeed
        assertNotNull(parser.parseOrNull(context, 0))

        // Now test a failing case - sequence succeeds up to "hello " then fails
        val context2 = DefaultParseContext("hello goodbye")
        val result2 = parser.parseOrNull(context2, 0)
        assertNull(result2)
        // "world" was attempted at position 6 (after "hello ") and failed
        assertEquals(6, context2.errorPosition)
    }

    @Test
    fun suggestedParsersWithNamedParsers() {
        // Named parsers appear in suggestedParsers when they fail
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val parser = letter + digit

        val context = DefaultParseContext("@invalid")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(0, context.errorPosition)
        // Both parsers failed at position 0, both should be suggested
        assertTrue(context.suggestedParsers.any { it.name == "letter" })
        assertTrue(context.suggestedParsers.any { it.name == "digit" })
    }

    @Test
    fun suggestedParsersWithMultipleChoices() {
        // When multiple named parsers fail at the same position, all are suggested
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val underscore = +'_' named "underscore"
        val parser = letter + digit + underscore

        val context = DefaultParseContext("@invalid")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(0, context.errorPosition)
        // All three parsers failed at position 0
        assertTrue(context.suggestedParsers.any { it.name == "letter" })
        assertTrue(context.suggestedParsers.any { it.name == "digit" })
        assertTrue(context.suggestedParsers.any { it.name == "underscore" })
    }

    @Test
    fun suggestedParsersUpdatesAsFurtherPositionReached() {
        // suggestedParsers only contains parsers that failed at the errorPosition
        // When a sequence partially matches, the error position advances
        val hello = +"hello" named "hello"
        val space = +' '
        val world = +"world" named "world"
        val parser = hello * space * world

        val context = DefaultParseContext("hello test")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        // Parsing succeeded up to "hello ", then "world" was attempted at position 6
        assertEquals(6, context.errorPosition)
        // Only "world" should be in suggestions since it failed at the furthest position
        assertTrue(context.suggestedParsers?.any { it.name == "world" } == true)
        // "hello" should not be in suggestions since it succeeded
        assertTrue(context.suggestedParsers?.none { it.name == "hello" } == true)
    }

    @Test
    fun errorContextInIdentifierParser() {
        // Practical example: identifier must start with letter
        val letter = +Regex("[a-zA-Z]") named "letter" map { it.value }
        val letterOrDigit = +Regex("[a-zA-Z0-9]") named "letter_or_digit" map { it.value }
        val identifier = letter * letterOrDigit.zeroOrMore

        val context = DefaultParseContext("123abc")
        val result = identifier.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(0, context.errorPosition)
        // Should suggest "letter" at the start
        assertTrue(context.suggestedParsers?.any { it.name == "letter" } == true)
    }

    @Test
    fun errorContextWithComplexGrammar() {
        // Complex grammar example with multiple alternatives
        val number = +Regex("[0-9]+") named "number" map { it.value.toInt() }
        val lparen = +'('
        val rparen = +')'
        val plus = +'+'

        // Simplified expression: number or (number + number)
        val simple = number
        val complex = lparen * number * plus * number * rparen map { it.b }
        val expr = simple + complex

        val context = DefaultParseContext("(42+")
        val result = expr.parseOrNull(context, 0)

        assertNull(result)
        // Should fail after "(42+"
        assertEquals(4, context.errorPosition)
        // Should suggest "number" at position 4
        assertTrue(context.suggestedParsers?.any { it.name == "number" } == true)
    }

    @Test
    fun buildingErrorMessagesFromContext() {
        // Demonstrate how to build user-friendly error messages
        val letter = +Regex("[a-z]") named "lowercase_letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val parser = letter * digit

        val context = DefaultParseContext("a@")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)

        // Build an error message using context information
        val errorMessage = buildErrorMessage(context)

        // Check that the error message contains useful information
        assertTrue(errorMessage.contains("position 1"))
        assertTrue(errorMessage.contains("digit"))
    }

    @Test
    fun errorContextWithParseException() {
        // When using parseAll(...).getOrThrow(), you can catch the exception and inspect its context
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val parser = letter

        val exception = assertFailsWith<ParseException> {
            parser.parseAll("123").getOrThrow()
        }

        // Exception contains the context with error information
        assertEquals(0, exception.context.errorPosition)
        // suggestedParsers should contain parsers that failed
        assertTrue(exception.context.suggestedParsers?.isNotEmpty() == true)
        assertTrue(exception.context.suggestedParsers?.any { it.name == "letter" } == true)
        assertEquals(0, exception.context.errorPosition ?: 0)
    }

    @Test
    fun errorContextWithExtraCharacters() {
        // ParseException also provides context when extra characters are found
        val parser = +"hello" named "greeting"

        val exception = assertFailsWith<ParseException> {
            parser.parseAll("helloworld").getOrThrow()
        }

        // Exception contains the context
        // Position points to errorPosition (where EOF was expected after "hello")
        assertEquals(5, exception.context.errorPosition ?: 0)
        // suggestedParsers should contain "EOF"
        assertTrue(exception.context.suggestedParsers?.any { it.name == "EOF" } == true)
    }

    @Test
    fun errorContextWithUnnamedParsers() {
        // Even without named parsers, errorPosition is tracked
        val parser = +"hello" * +"world"
        val context = DefaultParseContext("hellotest")
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(5, context.errorPosition)
        // suggestedParsers will contain unnamed parsers
        assertTrue(context.suggestedParsers?.isNotEmpty() == true)
    }

    @Test
    fun independentParseContextsHaveSeparateErrorState() {
        // Each ParseContext is independent and maintains different error positions
        val hello = +"hello" named "greeting"
        val world = +"world" named "farewell"

        // Test with partial match advancing error position
        val parser = hello * +' ' * world

        val context1 = DefaultParseContext("hello test")
        parser.parseOrNull(context1, 0)
        // Failed at position 6 (after "hello ")
        assertEquals(6, context1.errorPosition)

        val context2 = DefaultParseContext("goodbye")
        parser.parseOrNull(context2, 0)
        // Failed at position 0 (first parser didn't match)
        assertEquals(0, context2.errorPosition)

        // The two contexts are independent
        assertEquals(6, context1.errorPosition)
        assertEquals(0, context2.errorPosition)
    }

    @Test
    fun formatMessageForUnmatchedInput() {
        // Test formatMessage for ParseException (unmatched input case)
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val parser = letter * digit

        val input = "123"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val formatted = exception.formatMessage()

        // Should contain error information in new format
        assertTrue(formatted.contains("Syntax Error at 1:1"))
        // Should contain the source line
        assertTrue(formatted.contains("123"))
        // Should contain caret indicator
        assertTrue(formatted.contains("^"))
        // Should contain expected parsers
        assertTrue(formatted.contains("letter"))
    }

    @Test
    fun formatMessageForMultilineInput() {
        // Test formatMessage with multiline input
        val hello = +"hello"
        val space = +' '
        val world = +"world" named "world"
        val parser = hello * space * world

        val input = "hello test\nline2"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val formatted = exception.formatMessage()

        // Should point to the correct position in new format
        assertTrue(formatted.contains("Syntax Error at 1:7"))
        // Should show the problematic line
        assertTrue(formatted.contains("hello test"))
        // Should show caret at error position
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageForExtraCharacters() {
        // Test formatMessage for ParseException (extra characters case)
        val parser = +"hello" named "greeting"

        val input = "helloworld"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val formatted = exception.formatMessage()

        // Should contain error information in new format
        val lines = formatted.lines()
        assertTrue(lines[0].contains("Syntax Error at"))
        // Should contain the source line
        assertTrue(formatted.contains("helloworld"))
        // Should contain caret indicator
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageWithNamedParser() {
        // Test formatMessage when named parser is available
        val parser = +"test" named "test"

        val input = "fail"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val formatted = exception.formatMessage()

        // Should contain error information with expected parser name
        assertTrue(formatted.contains("Syntax Error at"))
        assertTrue(formatted.contains("Expect:"))
        assertTrue(formatted.contains("test"))
        assertTrue(formatted.contains("fail"))
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageWithEmptyLine() {
        // Test formatMessage with error on empty line
        val parser = +"test"
        val input = "\n"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Should contain error information in new format
        assertTrue(lines[0].contains("Syntax Error at"))
        // Should have Expect line
        assertTrue(lines[1].startsWith("Expect:"))
        // Should have Actual line with escaped newline
        assertEquals("Actual: \"\\n\"", lines[2])
        // Empty line (source line display for newline character)
        assertEquals("", lines[3])
        // Caret should be at position 0
        assertEquals("^", lines[4])
    }

    @Test
    fun formatMessageWithNamelessFixedParser() {
        // Use a StringParser which has a name (the string itself)
        // This test verifies that parsers with names are shown in Expect line
        val parser = +"test"

        val input = "fail"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Should have suggested parsers and Expect line
        assertTrue(exception.context.suggestedParsers?.isNotEmpty() == true)
        val expectLine = lines.find { it.startsWith("Expect:") }
        assertNotNull(expectLine)
        assertTrue(expectLine.contains("test"))
    }

    @Test
    fun formatMessageWithMixedLineEndings() {
        // Test with mixed line endings (LF, CRLF, and positions after \r)
        val parser = +"test"
        val input = "hello\r\nworld\ntest\rfail"
        val exception = assertFailsWith<ParseException> {
            parser.parseAll(input).getOrThrow()
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Should display error information correctly even with mixed line endings in new format
        assertTrue(lines[0].contains("Syntax Error at"))

        // Caret position should be correctly calculated
        val caretLine = lines.find { it.trim().startsWith("^") }
        assertNotNull(caretLine, "Should have a caret line")
    }

    // Helper function to build error messages from context
    private fun buildErrorMessage(context: ParseContext): String {
        val suggestions = context.suggestedParsers.orEmpty()
            .mapNotNull { it.name }
            .distinct()
            .sorted()
            .joinToString(", ")

        return if (suggestions.isNotEmpty()) {
            "Failed to parse at position ${context.errorPosition}. Expected: $suggestions"
        } else {
            "Failed to parse at position ${context.errorPosition}."
        }
    }

    @Test
    fun formatMessageDocExample() {
        // Test the exact example from documentation
        val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
        val operator = +'+' + +'-'
        val expr = number * operator * number

        val input = "42*10"
        val exception = assertFailsWith<ParseException> {
            expr.parseAll(input).getOrThrow()
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Verify the exact structure from documentation (matching actual formatMessage output)
        assertEquals("Syntax Error at 1:3", lines[0])
        assertEquals("Expect: \"+\", \"-\"", lines[1])
        assertEquals("Actual: \"*\"", lines[2])
        assertEquals("42*10", lines[3])
        assertEquals("  ^", lines[4])
    }
}
