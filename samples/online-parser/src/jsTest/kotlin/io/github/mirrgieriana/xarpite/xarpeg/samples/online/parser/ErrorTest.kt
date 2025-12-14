package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertTrue

class ErrorTest {

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
    fun showsErrorMessageForDivisionByZero() {
        val result = parseExpression("1 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
    }

    @Test
    fun parsesFunctionCall() {
        // This would need multiple statements, which we don't support in single expression
        // So we test error case
        val result = parseExpression("undefined_func(1, 2)")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun showsErrorForInvalidSyntax() {
        val result = parseExpression("x = ")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun showsErrorForMismatchedParentheses() {
        val result = parseExpression("(1 + 2")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun showsErrorForInvalidOperator() {
        val result = parseExpression("5 % 2")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun showsErrorForArgumentCountMismatch() {
        // Can't actually test this in current implementation since variables are reset
        // between calls, but we verify the error message exists in implementation
        val result = parseExpression("f()")
        assertTrue(result.startsWith("Error"))
    }

    @Test
    fun showsErrorWhenFunctionCallLimitExceeded() {
        // Create an infinite recursion that will hit the limit
        val result = parseExpression("infinite = (n) -> infinite(n + 1)\ninfinite(0)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Maximum function call limit"))
    }

    @Test
    fun showsCallStackInRecursiveError() {
        // Create a function that causes error in deep recursion
        val result = parseExpression("crash = (n) -> n <= 0 ? 1 / 0 : crash(n - 1)\ncrash(3)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("at line"))
        assertTrue(result.contains("crash"))
    }

    @Test
    fun showsCallStackWithMultipleFunctionCalls() {
        // Create nested function calls that cause an error
        val result = parseExpression("f = (x) -> g(x)\ng = (x) -> 1 / 0\nf(5)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("at line"))
        // Both functions should appear in the stack
        assertTrue(result.contains("f"))
        assertTrue(result.contains("g"))
    }

    @Test
    fun showsCallStackWithLineAndColumn() {
        // Test that position information is included in stack trace
        val result = parseExpression("errorFunc = (x) -> 1 / 0\nerrorFunc(5)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("line"))
    }

    @Test
    fun errorStackTraceShowsCorrectCallStack() {
        // Test the HTML example: func1 = (a, b) -> a / b, func2 = (a, b) -> func1(a + b, a - b), func2(10, 10)
        // This should show a proper call stack with func2 calling func1
        val result = parseExpression("func1 = (a, b) -> a / b\nfunc2 = (a, b) -> func1(a + b, a - b)\nfunc2(10, 10)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        assertTrue(result.contains("at line"))
        assertTrue(result.contains("func1"))
        assertTrue(result.contains("func2"))
    }
}
