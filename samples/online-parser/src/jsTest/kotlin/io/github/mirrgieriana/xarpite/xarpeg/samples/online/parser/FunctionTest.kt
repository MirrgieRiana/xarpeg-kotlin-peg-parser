package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionTest {

    @Test
    fun parsesLambdaExpression() {
        val result = parseExpression("add = (a, b) -> a + b")
        assertTrue(result.contains("lambda"))
    }

    @Test
    fun parsesLambdaWithNoParameters() {
        val result = parseExpression("f = () -> 42")
        assertTrue(result.contains("lambda()"))
    }

    @Test
    fun parsesLambdaWithOneParameter() {
        val result = parseExpression("double = (x) -> x * 2")
        assertTrue(result.contains("lambda(x)"))
    }

    @Test
    fun parsesLambdaWithMultipleParameters() {
        val result = parseExpression("add3 = (a, b, c) -> a + b + c")
        assertTrue(result.contains("lambda(a, b, c)"))
    }

    @Test
    fun parsesLambdaWithWhitespaceInParameters() {
        val result = parseExpression("add = ( a , b ) -> a + b")
        assertTrue(result.contains("lambda(a, b)"))
    }

    @Test
    fun parsesLambdaWithComplexBody() {
        val result = parseExpression("calc = (x, y) -> (x + y) * 2 - 1")
        assertTrue(result.contains("lambda(x, y)"))
    }

    @Test
    fun parsesLambdaAssignmentReturnsLambda() {
        val result = parseExpression("sum = (a, b) -> a + b")
        assertTrue(result.startsWith("<lambda"))
        assertTrue(result.contains("a, b"))
    }

    @Test
    fun parsesRecursiveFactorial() {
        val result = parseExpression("factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(5)")
        assertEquals("120", result)
    }

    @Test
    fun parsesRecursiveFactorialSmallNumber() {
        val result = parseExpression("fact = (n) -> n <= 1 ? 1 : n * fact(n - 1)\nfact(3)")
        assertEquals("6", result)
    }

    @Test
    fun parsesRecursiveFactorialZero() {
        val result = parseExpression("factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(0)")
        assertEquals("1", result)
    }

    @Test
    fun parsesRecursiveFibonacci() {
        val result = parseExpression("fib = (n) -> n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)")
        assertEquals("13", result)
    }

    @Test
    fun functionCallDoesNotOverwriteCallerVariablesWithSameArgumentName() {
        // Test that calling a function with an argument name that matches a caller's variable
        // doesn't destroy the caller's variable value
        val result = parseExpression("outer = (a) -> inner(10) + a\ninner = (a) -> a * 2\nouter(5)")
        assertEquals("25", result)  // inner returns 20, outer should still have a=5, so 20 + 5 = 25
    }

    @Test
    fun nestedFunctionCallsPreserveVariableScopes() {
        // Test more complex nested function calls with same parameter names
        val result = parseExpression("f = (x) -> g(x + 1) + x\ng = (x) -> h(x * 2) + x\nh = (x) -> x - 5\nf(2)")
        // f(2): calls g(3), which calls h(6), which returns 1
        // g gets h's result (1) and adds x=3: 1 + 3 = 4
        // f gets g's result (4) and adds x=2: 4 + 2 = 6
        assertEquals("6", result)
    }

    @Test
    fun recursiveFunctionPreservesVariableScope() {
        // Test that recursive calls properly maintain separate variable scopes
        val result = parseExpression("sum = (n, acc) -> n <= 0 ? acc : sum(n - 1, acc + n)\nsum(5, 0)")
        assertEquals("15", result)  // 5 + 4 + 3 + 2 + 1 = 15
    }
}
