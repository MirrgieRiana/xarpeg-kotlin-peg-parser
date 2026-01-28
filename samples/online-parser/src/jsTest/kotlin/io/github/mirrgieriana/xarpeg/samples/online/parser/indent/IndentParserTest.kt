package io.github.mirrgieriana.xarpeg.samples.online.parser.indent

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the indent-based parser example
 */
class IndentParserTest {

    @Test
    fun testSimpleFunction() {
        val input = "fun hello:\n    world"
        val result = IndentParser.program.parseAllWithIndentOrThrow(input)
        assertEquals(1, result.size)
        assertEquals("hello", result[0].name)
        assertEquals(1, result[0].body.size)
        assertEquals("world", (result[0].body[0] as ExpressionNode).value)
    }

    @Test
    fun testFunctionWithMultipleStatements() {
        val input = "fun test:\n    a\n    b\n    c"
        val result = IndentParser.program.parseAllWithIndentOrThrow(input)
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
        assertEquals(3, result[0].body.size)
        assertEquals("a", (result[0].body[0] as ExpressionNode).value)
        assertEquals("b", (result[0].body[1] as ExpressionNode).value)
        assertEquals("c", (result[0].body[2] as ExpressionNode).value)
    }

    @Test
    fun testEmptyFunction() {
        val input = "fun empty:\nfun another:\n    x"
        val result = IndentParser.program.parseAllWithIndentOrThrow(input)
        assertEquals(2, result.size)
        assertEquals("empty", result[0].name)
        assertEquals(0, result[0].body.size)
        assertEquals("another", result[1].name)
        assertEquals(1, result[1].body.size)
        assertEquals("x", (result[1].body[0] as ExpressionNode).value)
    }

    @Test
    fun testIndentParseContextStackManagement() {
        val context = IndentParseContext("test", useMemoization = false)
        assertEquals(0, context.currentIndent)

        context.pushIndent(4)
        assertEquals(4, context.currentIndent)

        context.pushIndent(8)
        assertEquals(8, context.currentIndent)

        context.popIndent()
        assertEquals(4, context.currentIndent)

        context.popIndent()
        assertEquals(0, context.currentIndent)
    }
}
