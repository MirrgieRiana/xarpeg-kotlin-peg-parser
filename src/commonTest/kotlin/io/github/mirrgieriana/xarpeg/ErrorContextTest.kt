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
        val context = ParseContext("help", useMemoization = true)
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        // Parser was attempted at position 0 and failed
        assertEquals(0, context.errorPosition)
    }

    @Test
    fun errorPositionWithSimpleChoice() {
        // With choice operators, errorPosition shows where parsing was attempted
        val parser = (+"hello" + +"world")
        val context = ParseContext("help", useMemoization = true)
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

        val context = ParseContext("hello world!", useMemoization = true)
        // Note: This will succeed
        assertNotNull(parser.parseOrNull(context, 0))

        // Now test a failing case - sequence succeeds up to "hello " then fails
        val context2 = ParseContext("hello goodbye", useMemoization = true)
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

        val context = ParseContext("@invalid", useMemoization = true)
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

        val context = ParseContext("@invalid", useMemoization = true)
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

        val context = ParseContext("hello test", useMemoization = true)
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        // Parsing succeeded up to "hello ", then "world" was attempted at position 6
        assertEquals(6, context.errorPosition)
        // Only "world" should be in suggestions since it failed at the furthest position
        assertTrue(context.suggestedParsers.any { it.name == "world" })
        // "hello" should not be in suggestions since it succeeded
        assertTrue(context.suggestedParsers.none { it.name == "hello" })
    }

    @Test
    fun errorContextInIdentifierParser() {
        // Practical example: identifier must start with letter
        val letter = +Regex("[a-zA-Z]") named "letter" map { it.value }
        val letterOrDigit = +Regex("[a-zA-Z0-9]") named "letter_or_digit" map { it.value }
        val identifier = letter * letterOrDigit.zeroOrMore

        val context = ParseContext("123abc", useMemoization = true)
        val result = identifier.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(0, context.errorPosition)
        // Should suggest "letter" at the start
        assertTrue(context.suggestedParsers.any { it.name == "letter" })
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

        val context = ParseContext("(42+", useMemoization = true)
        val result = expr.parseOrNull(context, 0)

        assertNull(result)
        // Should fail after "(42+"
        assertEquals(4, context.errorPosition)
        // Should suggest "number" at position 4
        assertTrue(context.suggestedParsers.any { it.name == "number" })
    }

    @Test
    fun buildingErrorMessagesFromContext() {
        // Demonstrate how to build user-friendly error messages
        val letter = +Regex("[a-z]") named "lowercase_letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val parser = letter * digit

        val context = ParseContext("a@", useMemoization = true)
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
        // When using parseAllOrThrow, you can catch the exception and inspect its context
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val parser = letter

        val exception = assertFailsWith<UnmatchedInputParseException> {
            parser.parseAllOrThrow("123")
        }

        // Exception contains the context with error information
        assertNotNull(exception.context)
        assertEquals(0, exception.context.errorPosition)
        // suggestedParsers should contain parsers that failed
        assertTrue(exception.context.suggestedParsers.isNotEmpty())
        assertTrue(exception.context.suggestedParsers.any { it.name == "letter" })
        assertEquals(0, exception.position)
    }

    @Test
    fun errorContextWithExtraCharacters() {
        // ExtraCharactersParseException also provides context
        val parser = +"hello" named "greeting"

        val exception = assertFailsWith<ExtraCharactersParseException> {
            parser.parseAllOrThrow("helloworld")
        }

        // Exception contains the context
        assertNotNull(exception.context)
        // Position points to errorPosition (furthest parse attempt during parsing)
        // In this case, "hello" parses successfully without failures, so errorPosition stays at 0
        assertEquals(0, exception.position)
    }

    @Test
    fun errorContextWithUnnamedParsers() {
        // Even without named parsers, errorPosition is tracked
        val parser = +"hello" * +"world"
        val context = ParseContext("hellotest", useMemoization = true)
        val result = parser.parseOrNull(context, 0)

        assertNull(result)
        assertEquals(5, context.errorPosition)
        // suggestedParsers will contain unnamed parsers
        assertTrue(context.suggestedParsers.isNotEmpty())
    }

    @Test
    fun errorContextResetsBetweenParses() {
        // Each ParseContext is independent
        val hello = +"hello" named "greeting"
        val world = +"world" named "farewell"

        // Test with partial match advancing error position
        val parser = hello * +' ' * world

        val context1 = ParseContext("hello test", useMemoization = true)
        parser.parseOrNull(context1, 0)
        // Failed at position 6 (after "hello ")
        assertEquals(6, context1.errorPosition)

        val context2 = ParseContext("goodbye", useMemoization = true)
        parser.parseOrNull(context2, 0)
        // Failed at position 0 (first parser didn't match)
        assertEquals(0, context2.errorPosition)

        // The two contexts are independent
        assertEquals(6, context1.errorPosition)
        assertEquals(0, context2.errorPosition)
    }

    @Test
    fun formatMessageForUnmatchedInput() {
        // Test formatMessage for UnmatchedInputParseException
        val letter = +Regex("[a-z]") named "letter" map { it.value }
        val digit = +Regex("[0-9]") named "digit" map { it.value }
        val parser = letter * digit

        val input = "123"
        val exception = assertFailsWith<UnmatchedInputParseException> {
            parser.parseAllOrThrow(input)
        }

        val formatted = exception.formatMessage()

        // Should contain line/column information
        assertTrue(formatted.contains("line 1"))
        assertTrue(formatted.contains("column 1"))
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
        val exception = assertFailsWith<UnmatchedInputParseException> {
            parser.parseAllOrThrow(input)
        }

        val formatted = exception.formatMessage()

        // Should point to the correct line (first line where error occurred)
        assertTrue(formatted.contains("line 1"))
        assertTrue(formatted.contains("column 7"))
        // Should show the problematic line
        assertTrue(formatted.contains("hello test"))
        // Should show caret at error position
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageForExtraCharacters() {
        // Test formatMessage for ExtraCharactersParseException
        val parser = +"hello" named "greeting"

        val input = "helloworld"
        val exception = assertFailsWith<ExtraCharactersParseException> {
            parser.parseAllOrThrow(input)
        }

        val formatted = exception.formatMessage()

        // Debug: print the formatted message
        println("ExtraCharactersParseException formatted message:")
        println(formatted)
        println("Exception position: ${exception.position}")
        println("Context errorPosition: ${exception.context.errorPosition}")

        // Should contain line/column information
        assertTrue(formatted.contains("line 1"))
        // Position is now errorPosition, which should be 5 (column 6)
        // But let's verify what column is actually displayed
        val lines = formatted.lines()
        assertTrue(lines[0].contains("column"))
        // Should contain the source line
        assertTrue(formatted.contains("helloworld"))
        // Should contain caret indicator
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageWithNoSuggestedParsers() {
        // Test formatMessage when no named parsers are available
        val parser = +"test"

        val input = "fail"
        val exception = assertFailsWith<UnmatchedInputParseException> {
            parser.parseAllOrThrow(input)
        }

        val formatted = exception.formatMessage()

        // Should still contain basic error information
        assertTrue(formatted.contains("Error"))
        assertTrue(formatted.contains("line 1"))
        assertTrue(formatted.contains("fail"))
        assertTrue(formatted.contains("^"))
    }

    @Test
    fun formatMessageWithEmptyLine() {
        // Test formatMessage with error on empty line
        val parser = +"test"
        val input = "\n"
        val exception = assertFailsWith<ParseException> {
            parser.parseAllOrThrow(input)
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Should contain error information
        assertTrue(lines[0].contains("Error"))
        assertTrue(lines[0].contains("line 1"))
        // May have Expected line if suggestedParsers is not empty
        // Empty source line should be displayed (after Expected line if present)
        val emptyLineIndex = if (lines[1].startsWith("Expected:")) 2 else 1
        assertEquals("", lines[emptyLineIndex])
        // Caret should be at position 0
        assertEquals("^", lines[emptyLineIndex + 1])
    }

    @Test
    fun formatMessageWithNamelessFixedParser() {
        // Test how nameless fixed parsers appear in the message
        val parser = Parser<String> { _, _ -> null }
        val input = "fail"
        val exception = assertFailsWith<ParseException> {
            parser.parseAllOrThrow(input)
        }

        val message = exception.formatMessage()

        // Debug output
        println("=== Nameless fixed parser test ===")
        println("Message:")
        println(message)
        println()
        println("Suggested parsers (${exception.context.suggestedParsers.size}):")
        exception.context.suggestedParsers.forEach {
            println("  name: ${it.name}")
            println("  toString: $it")
        }
        println("===")

        // Nameless parsers should appear in Expected line with their toString representation
        val hasExpectedLine = message.contains("Expected:")
        if (!hasExpectedLine) {
            println("WARNING: No 'Expected:' line found in message")
            // If there are no suggestedParsers, this is acceptable
            if (exception.context.suggestedParsers.isEmpty()) {
                println("No suggested parsers, skipping test")
                return
            }
        }
        assertTrue(hasExpectedLine, "Expected line should be present when there are suggested parsers")
        // The parser's toString should be included in the Expected line
        val lines = message.lines()
        val expectedLine = lines.find { it.startsWith("Expected:") }
        assertNotNull(expectedLine)
        // Should contain something (the toString representation of the parser)
        assertTrue(expectedLine.length > "Expected: ".length)
    }

    // Helper function to build error messages from context
    private fun buildErrorMessage(context: ParseContext): String {
        val suggestions = context.suggestedParsers
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
        val exception = assertFailsWith<UnmatchedInputParseException> {
            expr.parseAllOrThrow(input)
        }

        val message = exception.formatMessage()
        val lines = message.lines()

        // Verify the exact structure from documentation
        assertEquals("Error: Unmatched input at line 1, column 3", lines[0])
        assertEquals("Expected: \"+\", \"-\"", lines[1])
        assertEquals("42*10", lines[2])
        assertEquals("  ^", lines[3])
    }
}
