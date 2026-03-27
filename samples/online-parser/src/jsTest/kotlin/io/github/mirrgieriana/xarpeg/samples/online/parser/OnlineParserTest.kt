package io.github.mirrgieriana.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnlineParserTest {

    // -- Arithmetic --

    @Test
    fun arithmetic() {
        assertEquals("3", evaluateExpression("1 + 2").output)
        assertEquals("14", evaluateExpression("2 * ( 3 + 4 )").output)
        assertEquals("23", evaluateExpression("(5 + 3) * 2 + 7").output)
        assertEquals("14", evaluateExpression("((2 + 3) * 2) + 4").output)
        assertEquals("-5", evaluateExpression("5 - 10").output)
        assertEquals("0", evaluateExpression("0").output)
        assertEquals("1000000", evaluateExpression("1000000").output)
    }

    @Test
    fun operatorPrecedence() {
        assertEquals("11", evaluateExpression("5 + 2 * 3").output)
        assertEquals("7", evaluateExpression("5 + 6 / 3").output)
        assertEquals("1", evaluateExpression("10 - 3 * 3").output)
    }

    @Test
    fun decimalNumbers() {
        assertEquals("3.14", evaluateExpression("3.14").output)
        assertEquals("5.5", evaluateExpression("2.5 + 3.0").output)
        assertEquals("0.001", evaluateExpression("0.001").output)
    }

    // -- Whitespace --

    @Test
    fun whitespace() {
        assertEquals("3", evaluateExpression(" 1+2").output)
        assertEquals("3", evaluateExpression("1+2 ").output)
        assertEquals("3", evaluateExpression(" 1+2 ").output)
        assertEquals("10", evaluateExpression("  5   +   5  ").output)
        assertEquals("8", evaluateExpression("4\t+\t4").output)
        assertEquals("6", evaluateExpression("2 *\n3").output)
    }

    // -- Variables --

    @Test
    fun variableDeclaration() {
        assertEquals("5", evaluateExpression("var x = 5\nx").output)
        assertEquals("15", evaluateExpression("var x = 5 + 10\nx").output)
        assertEquals("1.5", evaluateExpression("var pi_half = 1.5\npi_half").output)
        assertEquals("true", evaluateExpression("var result = 5 > 3\nresult").output)
        assertEquals("100", evaluateExpression("var x = 5 > 3 ? 100 : 200\nx").output)
    }

    @Test
    fun identifiers() {
        assertEquals("42", evaluateExpression("var my_var = 42\nmy_var").output)
        assertEquals("100", evaluateExpression("var var123 = 100\nvar123").output)
        assertEquals("99", evaluateExpression("var _private = 99\n_private").output)
        assertEquals("77", evaluateExpression("var __value__ = 77\n__value__").output)
        assertEquals("88", evaluateExpression("var MyVariable = 88\nMyVariable").output)
        assertEquals("55", evaluateExpression("var var2 = 55\nvar2").output)
    }

    @Test
    fun variableReassignment() {
        assertEquals("10", evaluateExpression("var x = 5\nx = 10\nx").output)
        assertEquals("3", evaluateExpression("var x = 1\nvar y = 2\nx = x + y\nx").output)
    }

    // -- Lambda --

    @Test
    fun lambda() {
        assertTrue(evaluateExpression("() -> 42").output.contains("lambda()"))
        assertTrue(evaluateExpression("(x) -> x * 2").output.contains("lambda(x)"))
        assertTrue(evaluateExpression("(a, b) -> a + b").output.contains("lambda(a, b)"))
        assertTrue(evaluateExpression("(a, b, c) -> a + b + c").output.contains("lambda(a, b, c)"))
        assertTrue(evaluateExpression("( a , b ) -> a + b").output.contains("lambda(a, b)"))
    }

    // -- Comparison --

    @Test
    fun equalityComparison() {
        assertEquals("true", evaluateExpression("5 == 5").output)
        assertEquals("false", evaluateExpression("5 == 3").output)
        assertEquals("false", evaluateExpression("5 != 5").output)
        assertEquals("true", evaluateExpression("5 != 3").output)
    }

    @Test
    fun orderingComparison() {
        assertEquals("true", evaluateExpression("3 < 5").output)
        assertEquals("false", evaluateExpression("5 < 3").output)
        assertEquals("false", evaluateExpression("5 < 5").output)
        assertEquals("true", evaluateExpression("3 <= 5").output)
        assertEquals("false", evaluateExpression("5 <= 3").output)
        assertEquals("true", evaluateExpression("5 <= 5").output)
        assertEquals("true", evaluateExpression("5 > 3").output)
        assertEquals("false", evaluateExpression("3 > 5").output)
        assertEquals("false", evaluateExpression("5 > 5").output)
        assertEquals("true", evaluateExpression("5 >= 3").output)
        assertEquals("false", evaluateExpression("3 >= 5").output)
        assertEquals("true", evaluateExpression("5 >= 5").output)
    }

    @Test
    fun comparisonWithExpressions() {
        assertEquals("true", evaluateExpression("2 + 3 == 5").output)
        assertEquals("false", evaluateExpression("2 * 3 == 5").output)
        assertEquals("true", evaluateExpression("10 - 5 > 3").output)
        // Left-associative: (5 > 3) == (10 > 5) = true == true = true
        assertEquals("true", evaluateExpression("5 > 3 == 10 > 5").output)
    }

    @Test
    fun booleanEquality() {
        assertEquals("true", evaluateExpression("(5 > 3) == (10 > 5)").output)
        assertEquals("false", evaluateExpression("(5 > 3) == (10 < 5)").output)
        assertEquals("false", evaluateExpression("(5 > 3) != (10 > 5)").output)
        assertEquals("true", evaluateExpression("(5 > 3) != (10 < 5)").output)
    }

    // -- Ternary --

    @Test
    fun ternary() {
        assertEquals("10", evaluateExpression("5 > 3 ? 10 : 20").output)
        assertEquals("20", evaluateExpression("5 < 3 ? 10 : 20").output)
        assertEquals("15", evaluateExpression("2 + 3 == 5 ? 10 + 5 : 20 + 5").output)
        assertEquals("1", evaluateExpression("5 > 3 ? (2 > 1 ? 1 : 2) : 3").output)
        assertEquals("15", evaluateExpression("5 + (10 > 5 ? 10 : 0)").output)
    }

    // -- Functions --

    @Test
    fun lambdaCall() {
        assertEquals("42", evaluateExpression("var f = () -> 42\nf()").output)
        assertEquals("10", evaluateExpression("var double = (x) -> x * 2\ndouble(5)").output)
        assertEquals("9", evaluateExpression("var add = (a, b) -> a + b\nadd(4, 5)").output)
    }

    @Test
    fun recursion() {
        assertEquals("120", evaluateExpression("var factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(5)").output)
        assertEquals("1", evaluateExpression("var factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(0)").output)
        assertEquals("13", evaluateExpression("var fib = (n) -> n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)").output)
    }

    @Test
    fun variableScoping() {
        assertEquals("25", evaluateExpression("var inner = (a) -> a * 2\nvar outer = (a) -> inner(10) + a\nouter(5)").output)
        assertEquals("6", evaluateExpression("var h = (x) -> x - 5\nvar g = (x) -> h(x * 2) + x\nvar f = (x) -> g(x + 1) + x\nf(2)").output)
        assertEquals("15", evaluateExpression("var sum = (n, acc) -> n <= 0 ? acc : sum(n - 1, acc + n)\nsum(5, 0)").output)
    }

    // -- Type errors --

    @Test
    fun arithmeticTypeError() {
        val result = evaluateExpression("5 + (3 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("is not defined for"))
    }

    @Test
    fun ternaryConditionTypeError() {
        val result = evaluateExpression("5 ? 10 : 20")
        assertFalse(result.success)
        assertTrue(result.output.contains("must be Boolean, got"))
    }

    @Test
    fun comparisonTypeError() {
        val result = evaluateExpression("(5 > 3) < (10 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("is not defined for"))
    }

    @Test
    fun equalityTypeError() {
        val result = evaluateExpression("5 == (3 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("is not defined for"))
    }

    // -- Parse errors --

    @Test
    fun parseErrors() {
        assertFalse(evaluateExpression("x = ").success)
        assertFalse(evaluateExpression("(1 + 2").success)
        assertFalse(evaluateExpression("5 % 2").success)
    }

    // -- Runtime errors --

    @Test
    fun undefinedVariableError() {
        val result = evaluateExpression("undefined_var")
        assertFalse(result.success)
        assertTrue(result.output.contains("Undefined variable"))
    }

    @Test
    fun undeclaredVariableAssignmentError() {
        val result = evaluateExpression("x = 5")
        assertFalse(result.success)
        assertTrue(result.output.contains("Undefined variable"))
    }

    @Test
    fun undefinedFunctionError() {
        val result = evaluateExpression("f()")
        assertFalse(result.success)
        assertTrue(result.output.contains("Undefined function"))
    }

    @Test
    fun divisionByZeroError() {
        val result = evaluateExpression("1 / 0")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
    }

    @Test
    fun functionCallLimitError() {
        val result = evaluateExpression("var infinite = (n) -> infinite(n + 1)\ninfinite(0)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Maximum function call limit"))
    }

    // -- Stack traces --

    @Test
    fun callStackInRecursiveError() {
        val result = evaluateExpression("var crash = (n) -> n <= 0 ? 1 / 0 : crash(n - 1)\ncrash(3)")
        assertFalse(result.success)
        assertTrue(result.output.contains("at line"))
        assertTrue(result.output.contains("crash"))
    }

    @Test
    fun nestedCallStack() {
        val result = evaluateExpression("var g = (x) -> 1 / 0\nvar f = (x) -> g(x)\nf(5)")
        assertFalse(result.success)
        assertTrue(result.output.contains("at line"))
        assertTrue(result.output.contains("f"))
        assertTrue(result.output.contains("g"))
    }

    @Test
    fun errorLineNumbers() {
        val line2 = evaluateExpression("var x = 10\n1 / 0")
        assertFalse(line2.success)
        assertTrue(line2.output.contains("line 2"))

        val line3 = evaluateExpression("var x = 5\nvar y = 10\nvar z = x / (y - 10)")
        assertFalse(line3.success)
        assertTrue(line3.output.contains("line 3"))
    }

    @Test
    fun errorHighlighting() {
        val result = evaluateExpression("10 / 0")
        assertFalse(result.success)
        val stackLine = assertNotNull(result.output.split("\n").find { it.contains("at line") })
        assertTrue(stackLine.contains("[") && stackLine.contains("]"))
    }

    @Test
    fun errorHighlightSpansOperands() {
        val result = evaluateExpression("var a = 5 / 0")
        assertFalse(result.success)
        val highlighted = assertNotNull(result.output.split("\n").find { it.contains("at line") })
            .substringAfter("[").substringBefore("]")
        assertTrue(highlighted.contains("5"))
        assertTrue(highlighted.contains("/"))
        assertTrue(highlighted.contains("0"))
    }

    @Test
    fun fullCallStackWithLineNumbers() {
        val result = evaluateExpression("var func1 = (a, b) -> a / b\nvar func2 = (a, b) -> func1(a + b, a - b)\nfunc2(10, 10)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        assertTrue(result.output.contains("func1"))
        assertTrue(result.output.contains("func2"))
        assertTrue(result.output.contains("at line"))
    }
}
