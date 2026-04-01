package io.github.mirrgieriana.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HeredocTest {

    // -- Basic heredoc --

    @Test
    fun singleLineContent() {
        assertEquals("Hello", evaluateExpression("<< END\nHello\nEND").output)
    }

    @Test
    fun multiLineContent() {
        assertEquals("Hello\nWorld", evaluateExpression("<< END\nHello\nWorld\nEND").output)
    }

    @Test
    fun emptyContent() {
        assertEquals("", evaluateExpression("<< END\nEND").output)
    }

    // -- Delimiter variants --

    @Test
    fun differentDelimiters() {
        assertEquals("content", evaluateExpression("<< EOF\ncontent\nEOF").output)
        assertEquals("content", evaluateExpression("<< MARKER\ncontent\nMARKER").output)
        assertEquals("content", evaluateExpression("<< X\ncontent\nX").output)
    }

    @Test
    fun numericDelimiter() {
        assertEquals("content", evaluateExpression("<< 123\ncontent\n123").output)
    }

    // -- Delimiter matching --

    @Test
    fun delimiterMismatchFails() {
        assertFalse(evaluateExpression("<< END\ncontent\nEOF").success)
    }

    @Test
    fun delimiterPartialMatchIsContent() {
        assertEquals("ENDING", evaluateExpression("<< END\nENDING\nEND").output)
    }

    // -- Variable assignment --

    @Test
    fun assignToVariable() {
        assertEquals("Hello", evaluateExpression("var x = << END\nHello\nEND\nx").output)
    }

    @Test
    fun assignMultiLine() {
        assertEquals("Hello\nWorld", evaluateExpression("var x = << END\nHello\nWorld\nEND\nx").output)
    }

    // -- Inside indent block --

    @Test
    fun heredocInIndentFunction() {
        assertEquals("Hello", evaluateExpression("f():\n    << END\nHello\nEND\nf()").output)
    }

    @Test
    fun heredocAssignmentInIndentFunction() {
        assertEquals("Hello", evaluateExpression("f():\n    var x = << END\nHello\nEND\n    x\nf()").output)
    }

    @Test
    fun heredocInNestedIndent() {
        assertEquals("inner", evaluateExpression("f():\n    g():\n        << END\ninner\nEND\n    g()\nf()").output)
    }

    // -- Multiple heredocs --

    @Test
    fun sequentialHeredocs() {
        val code = "var a = << END\nHello\nEND\nvar b = << END\nWorld\nEND\na"
        assertEquals("Hello", evaluateExpression(code).output)
    }

    // -- Content edge cases --

    @Test
    fun contentWithEmptyLines() {
        assertEquals("Hello\n\nWorld", evaluateExpression("<< END\nHello\n\nWorld\nEND").output)
    }

    @Test
    fun contentWithSpaces() {
        assertEquals("  Hello  ", evaluateExpression("<< END\n  Hello  \nEND").output)
    }

    // -- Whitespace around delimiter --

    @Test
    fun whitespaceAroundOpenTagDelimiter() {
        assertEquals("Hello", evaluateExpression("<<  END \nHello\nEND").output)
    }

    // -- EOF handling --

    @Test
    fun closeTagAtEndOfFile() {
        assertEquals("Hello", evaluateExpression("<< END\nHello\nEND").output)
    }
}
