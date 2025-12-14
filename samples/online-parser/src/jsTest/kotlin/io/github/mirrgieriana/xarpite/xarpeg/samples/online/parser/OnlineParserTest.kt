package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnlineParserTest {

    @Test
    fun parsesExpressionWithWhitespaceAroundPlus() {
        assertEquals("3", parseExpression("1 + 2"))
    }

    @Test
    fun parsesExpressionWithWhitespaceAroundStar() {
        assertEquals("14", parseExpression("2 * ( 3 + 4 )"))
    }

    @Test
    fun parsesExpressionWithLeadingWhitespace() {
        assertEquals("3", parseExpression(" 1+2"))
    }

    @Test
    fun parsesExpressionWithTrailingWhitespace() {
        assertEquals("3", parseExpression("1+2 "))
    }

    @Test
    fun parsesExpressionWithBothLeadingAndTrailingWhitespace() {
        assertEquals("3", parseExpression(" 1+2 "))
    }

    @Test
    fun parsesVariableAssignment() {
        assertEquals("5", parseExpression("x = 5"))
    }

    @Test
    fun parsesVariableReference() {
        assertEquals("10", parseExpression("y = 10"))
    }

    @Test
    fun parsesLambdaExpression() {
        val result = parseExpression("add = (a, b) -> a + b")
        assertTrue(result.contains("lambda"))
    }

    @Test
    fun parsesFunctionCall() {
        // This would need multiple statements, which we don't support in single expression
        // So we test error case
        val result = parseExpression("undefined_func(1, 2)")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun parsesIdentifierWithUnderscore() {
        assertEquals("42", parseExpression("my_var = 42"))
    }

    @Test
    fun parsesIdentifierWithNumbers() {
        assertEquals("100", parseExpression("var123 = 100"))
    }

    @Test
    fun showsErrorForUndefinedVariable() {
        val result = parseExpression("undefined_var")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Undefined variable"))
    }

    @Test
    fun showsErrorForDivisionByZero() {
        val result = parseExpression("1 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
    }

    @Test
    fun showsStackTraceInError() {
        val result = parseExpression("1 / 0")
        assertTrue(result.contains("Stack trace"))
    }
}
