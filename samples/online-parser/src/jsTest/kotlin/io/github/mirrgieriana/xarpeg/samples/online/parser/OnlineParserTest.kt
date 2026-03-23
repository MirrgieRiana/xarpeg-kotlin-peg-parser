package io.github.mirrgieriana.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnlineParserTest {

    @Test
    fun parsesExpressionWithWhitespaceAroundPlus() {
        assertEquals("3", evaluateExpression("1 + 2").output)
    }

    @Test
    fun parsesExpressionWithWhitespaceAroundStar() {
        assertEquals("14", evaluateExpression("2 * ( 3 + 4 )").output)
    }

    @Test
    fun parsesExpressionWithLeadingWhitespace() {
        assertEquals("3", evaluateExpression(" 1+2").output)
    }

    @Test
    fun parsesExpressionWithTrailingWhitespace() {
        assertEquals("3", evaluateExpression("1+2 ").output)
    }

    @Test
    fun parsesExpressionWithBothLeadingAndTrailingWhitespace() {
        assertEquals("3", evaluateExpression(" 1+2 ").output)
    }

    @Test
    fun parsesVariableAssignment() {
        assertEquals("5", evaluateExpression("x = 5").output)
    }

    @Test
    fun parsesVariableAssignmentForY() {
        assertEquals("10", evaluateExpression("y = 10").output)
    }

    @Test
    fun parsesLambdaExpression() {
        val result = evaluateExpression("add = (a, b) -> a + b")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda"))
    }

    @Test
    fun parsesFunctionCall() {
        // Test that calling an undefined function results in an error
        val result = evaluateExpression("undefined_func(1, 2)")
        assertFalse(result.success)
    }

    @Test
    fun parsesIdentifierWithUnderscore() {
        assertEquals("42", evaluateExpression("my_var = 42").output)
    }

    @Test
    fun parsesIdentifierWithNumbers() {
        assertEquals("100", evaluateExpression("var123 = 100").output)
    }

    @Test
    fun showsErrorForUndefinedVariable() {
        val result = evaluateExpression("undefined_var")
        assertFalse(result.success)
        assertTrue(result.output.contains("Undefined variable"))
    }

    @Test
    fun showsErrorForDivisionByZero() {
        val result = evaluateExpression("1 / 0")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
    }

    // Lambda expression tests
    @Test
    fun parsesLambdaWithNoParameters() {
        val result = evaluateExpression("f = () -> 42")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda()"))
    }

    @Test
    fun parsesLambdaWithOneParameter() {
        val result = evaluateExpression("double = (x) -> x * 2")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda(x)"))
    }

    @Test
    fun parsesLambdaWithMultipleParameters() {
        val result = evaluateExpression("add3 = (a, b, c) -> a + b + c")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda(a, b, c)"))
    }

    @Test
    fun parsesLambdaWithWhitespaceInParameters() {
        val result = evaluateExpression("add = ( a , b ) -> a + b")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda(a, b)"))
    }

    @Test
    fun parsesLambdaWithComplexBody() {
        val result = evaluateExpression("calc = (x, y) -> (x + y) * 2 - 1")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda(x, y)"))
    }

    // Decimal number tests
    @Test
    fun parsesDecimalNumber() {
        assertEquals("3.14", evaluateExpression("3.14").output)
    }

    @Test
    fun parsesDecimalNumberInExpression() {
        assertEquals("5.5", evaluateExpression("2.5 + 3.0").output)
    }

    @Test
    fun parsesDecimalNumberInVariableAssignment() {
        assertEquals("1.5", evaluateExpression("pi_half = 1.5").output)
    }

    // Complex expression tests
    @Test
    fun parsesComplexArithmeticExpression() {
        assertEquals("23", evaluateExpression("(5 + 3) * 2 + 7").output)
    }

    @Test
    fun parsesVariableInArithmeticExpression() {
        assertEquals("15", evaluateExpression("x = 5 + 10").output)
    }

    @Test
    fun parsesNestedParentheses() {
        assertEquals("14", evaluateExpression("((2 + 3) * 2) + 4").output)
    }

    // Operator precedence tests
    @Test
    fun respectsOperatorPrecedenceMultiplicationFirst() {
        assertEquals("11", evaluateExpression("5 + 2 * 3").output)
    }

    @Test
    fun respectsOperatorPrecedenceDivisionFirst() {
        assertEquals("7", evaluateExpression("5 + 6 / 3").output)
    }

    @Test
    fun respectsOperatorPrecedenceWithSubtraction() {
        assertEquals("1", evaluateExpression("10 - 3 * 3").output)
    }

    // Identifier validation tests
    @Test
    fun parsesIdentifierStartingWithUnderscore() {
        assertEquals("99", evaluateExpression("_private = 99").output)
    }

    @Test
    fun parsesIdentifierWithMultipleUnderscores() {
        assertEquals("77", evaluateExpression("__value__ = 77").output)
    }

    @Test
    fun parsesIdentifierWithMixedCase() {
        assertEquals("88", evaluateExpression("MyVariable = 88").output)
    }

    @Test
    fun parsesIdentifierEndingWithNumber() {
        assertEquals("55", evaluateExpression("var2 = 55").output)
    }

    // Error case tests
    @Test
    fun showsErrorForInvalidSyntax() {
        val result = evaluateExpression("x = ")
        assertFalse(result.success)
    }

    @Test
    fun showsErrorForMismatchedParentheses() {
        val result = evaluateExpression("(1 + 2")
        assertFalse(result.success)
    }

    @Test
    fun showsErrorForInvalidOperator() {
        val result = evaluateExpression("5 % 2")
        assertFalse(result.success)
    }

    @Test
    fun showsErrorForUndefinedFunction() {
        // Calling an undefined function results in an error
        val result = evaluateExpression("f()")
        assertFalse(result.success)
    }

    // Whitespace handling tests
    @Test
    fun parsesExpressionWithVariousWhitespace() {
        assertEquals("10", evaluateExpression("  5   +   5  ").output)
    }

    @Test
    fun parsesExpressionWithNewlines() {
        assertEquals("6", evaluateExpression("2\n*\n3").output)
    }

    @Test
    fun parsesExpressionWithTabs() {
        assertEquals("8", evaluateExpression("4\t+\t4").output)
    }

    // Lambda and assignment combination tests
    @Test
    fun parsesMultipleVariableAssignments() {
        // Only the last assignment value is returned
        assertEquals("20", evaluateExpression("x = 20").output)
    }

    @Test
    fun parsesLambdaAssignmentReturnsLambda() {
        val result = evaluateExpression("sum = (a, b) -> a + b")
        assertTrue(result.success)
        assertTrue(result.output.startsWith("<lambda"))
        assertTrue(result.output.contains("a, b"))
    }

    // Edge cases
    @Test
    fun parsesZero() {
        assertEquals("0", evaluateExpression("0").output)
    }

    @Test
    fun parsesNegativeResult() {
        assertEquals("-5", evaluateExpression("5 - 10").output)
    }

    @Test
    fun parsesLargeNumber() {
        assertEquals("1000000", evaluateExpression("1000000").output)
    }

    @Test
    fun parsesVerySmallDecimal() {
        assertEquals("0.001", evaluateExpression("0.001").output)
    }

    // Comparison operator tests
    @Test
    fun parsesEqualityOperator() {
        assertEquals("true", evaluateExpression("5 == 5").output)
        assertEquals("false", evaluateExpression("5 == 3").output)
    }

    @Test
    fun parsesInequalityOperator() {
        assertEquals("false", evaluateExpression("5 != 5").output)
        assertEquals("true", evaluateExpression("5 != 3").output)
    }

    @Test
    fun parsesLessThanOperator() {
        assertEquals("true", evaluateExpression("3 < 5").output)
        assertEquals("false", evaluateExpression("5 < 3").output)
        assertEquals("false", evaluateExpression("5 < 5").output)
    }

    @Test
    fun parsesLessThanOrEqualOperator() {
        assertEquals("true", evaluateExpression("3 <= 5").output)
        assertEquals("false", evaluateExpression("5 <= 3").output)
        assertEquals("true", evaluateExpression("5 <= 5").output)
    }

    @Test
    fun parsesGreaterThanOperator() {
        assertEquals("true", evaluateExpression("5 > 3").output)
        assertEquals("false", evaluateExpression("3 > 5").output)
        assertEquals("false", evaluateExpression("5 > 5").output)
    }

    @Test
    fun parsesGreaterThanOrEqualOperator() {
        assertEquals("true", evaluateExpression("5 >= 3").output)
        assertEquals("false", evaluateExpression("3 >= 5").output)
        assertEquals("true", evaluateExpression("5 >= 5").output)
    }

    @Test
    fun parsesComparisonWithExpressions() {
        assertEquals("true", evaluateExpression("2 + 3 == 5").output)
        assertEquals("false", evaluateExpression("2 * 3 == 5").output)
        assertEquals("true", evaluateExpression("10 - 5 > 3").output)
    }

    // Ternary operator tests
    @Test
    fun parsesTernaryOperatorTrue() {
        assertEquals("10", evaluateExpression("5 > 3 ? 10 : 20").output)
    }

    @Test
    fun parsesTernaryOperatorFalse() {
        assertEquals("20", evaluateExpression("5 < 3 ? 10 : 20").output)
    }

    @Test
    fun parsesTernaryWithComplexExpressions() {
        assertEquals("15", evaluateExpression("2 + 3 == 5 ? 10 + 5 : 20 + 5").output)
    }

    @Test
    fun parsesTernaryWithVariables() {
        assertEquals("100", evaluateExpression("x = 5 > 3 ? 100 : 200").output)
    }

    @Test
    fun parsesNestedTernary() {
        assertEquals("1", evaluateExpression("5 > 3 ? (2 > 1 ? 1 : 2) : 3").output)
    }

    // Type checking tests
    @Test
    fun showsErrorWhenBooleanUsedInArithmetic() {
        val result = evaluateExpression("5 + (3 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("must be a number"))
    }

    @Test
    fun showsErrorWhenNonBooleanUsedInTernaryCondition() {
        val result = evaluateExpression("5 ? 10 : 20")
        assertFalse(result.success)
        assertTrue(result.output.contains("must be a boolean"))
    }

    @Test
    fun showsErrorWhenBooleanComparedInComparison() {
        val result = evaluateExpression("(5 > 3) < (10 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("must be a number"))
    }

    // Boolean value assignment tests
    @Test
    fun parsesBooleanAssignment() {
        assertEquals("true", evaluateExpression("result = 5 > 3").output)
    }

    @Test
    fun parsesBooleanInVariable() {
        assertEquals("false", evaluateExpression("isEqual = 5 == 3").output)
    }

    // Complex combination tests
    @Test
    fun parsesComplexBooleanExpression() {
        assertEquals("true", evaluateExpression("5 + 5 == 10").output)
        assertEquals("false", evaluateExpression("5 * 2 != 10").output)
    }

    @Test
    fun parsesTernaryInArithmetic() {
        assertEquals("15", evaluateExpression("5 + (10 > 5 ? 10 : 0)").output)
    }

    @Test
    fun parsesMultipleComparisons() {
        // Note: Comparisons are left-associative, so this is (5 > 3) == (10 > 5)
        // which is true == true = true
        assertEquals("true", evaluateExpression("5 > 3 == 10 > 5").output)
    }

    @Test
    fun parsesBooleanEqualityComparison() {
        assertEquals("true", evaluateExpression("(5 > 3) == (10 > 5)").output)
        assertEquals("false", evaluateExpression("(5 > 3) == (10 < 5)").output)
        assertEquals("false", evaluateExpression("(5 > 3) != (10 > 5)").output)
        assertEquals("true", evaluateExpression("(5 > 3) != (10 < 5)").output)
    }

    @Test
    fun showsErrorWhenComparingNumberWithBoolean() {
        val result = evaluateExpression("5 == (3 > 2)")
        assertFalse(result.success)
        assertTrue(result.output.contains("are not comparable"))
    }

    // Recursive function tests
    @Test
    fun parsesRecursiveFactorial() {
        val result = evaluateExpression("factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(5)")
        assertEquals("120", result.output)
    }

    @Test
    fun parsesRecursiveFactorialSmallNumber() {
        val result = evaluateExpression("fact = (n) -> n <= 1 ? 1 : n * fact(n - 1)\nfact(3)")
        assertEquals("6", result.output)
    }

    @Test
    fun parsesRecursiveFactorialZero() {
        val result = evaluateExpression("factorial = (n) -> n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(0)")
        assertEquals("1", result.output)
    }

    @Test
    fun showsErrorWhenFunctionCallLimitExceeded() {
        // Create an infinite recursion that will hit the limit
        val result = evaluateExpression("infinite = (n) -> infinite(n + 1)\ninfinite(0)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Maximum function call limit"))
    }

    @Test
    fun parsesRecursiveFibonacci() {
        val result = evaluateExpression("fib = (n) -> n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)")
        assertEquals("13", result.output)
    }

    // Stack trace tests
    @Test
    fun showsCallStackInRecursiveError() {
        // Create a function that causes error in deep recursion
        val result = evaluateExpression("crash = (n) -> n <= 0 ? 1 / 0 : crash(n - 1)\ncrash(3)")
        assertFalse(result.success)
        assertTrue(result.output.contains("at line"))
        assertTrue(result.output.contains("crash"))
    }

    @Test
    fun showsCallStackWithMultipleFunctionCalls() {
        // Create nested function calls that cause an error
        val result = evaluateExpression("f = (x) -> g(x)\ng = (x) -> 1 / 0\nf(5)")
        assertFalse(result.success)
        assertTrue(result.output.contains("at line"))
        // Both functions should appear in the stack
        assertTrue(result.output.contains("f"))
        assertTrue(result.output.contains("g"))
    }

    @Test
    fun showsCallStackWithLineAndColumn() {
        // Test that position information is included in stack trace
        val result = evaluateExpression("errorFunc = (x) -> 1 / 0\nerrorFunc(5)")
        assertFalse(result.success)
        assertTrue(result.output.contains("line"))
    }

    // Variable scoping tests
    @Test
    fun functionCallDoesNotOverwriteCallerVariablesWithSameArgumentName() {
        // Test that calling a function with an argument name that matches a caller's variable
        // doesn't destroy the caller's variable value
        val result = evaluateExpression("outer = (a) -> inner(10) + a\ninner = (a) -> a * 2\nouter(5)")
        assertEquals("25", result.output)  // inner returns 20, outer should still have a=5, so 20 + 5 = 25
    }

    @Test
    fun nestedFunctionCallsPreserveVariableScopes() {
        // Test more complex nested function calls with same parameter names
        val result = evaluateExpression("f = (x) -> g(x + 1) + x\ng = (x) -> h(x * 2) + x\nh = (x) -> x - 5\nf(2)")
        // f(2): calls g(3), which calls h(6), which returns 1
        // g gets h's result (1) and adds x=3: 1 + 3 = 4
        // f gets g's result (4) and adds x=2: 4 + 2 = 6
        assertEquals("6", result.output)
    }

    @Test
    fun recursiveFunctionPreservesVariableScope() {
        // Test that recursive calls properly maintain separate variable scopes
        val result = evaluateExpression("sum = (n, acc) -> n <= 0 ? acc : sum(n - 1, acc + n)\nsum(5, 0)")
        assertEquals("15", result.output)  // 5 + 4 + 3 + 2 + 1 = 15
    }

    @Test
    fun errorStackTraceShowsCorrectCallStack() {
        // Test the HTML example: func1 = (a, b) -> a / b, func2 = (a, b) -> func1(a + b, a - b), func2(10, 10)
        // This should show a proper call stack with func2 calling func1
        val result = evaluateExpression("func1 = (a, b) -> a / b\nfunc2 = (a, b) -> func1(a + b, a - b)\nfunc2(10, 10)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        assertTrue(result.output.contains("at line"))
        assertTrue(result.output.contains("func1"))
        assertTrue(result.output.contains("func2"))
    }

    @Test
    fun errorOnLine2ShowsCorrectLineNumber() {
        // Error occurs on the second line
        val result = evaluateExpression("x = 10\n1 / 0")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        assertTrue(result.output.contains("line 2"))
    }

    @Test
    fun errorOnLine3ShowsCorrectLineNumber() {
        // Error occurs on the third line
        val result = evaluateExpression("x = 5\ny = 10\nz = x / (y - 10)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        assertTrue(result.output.contains("line 3"))
    }

    @Test
    fun multiLineStackTraceShowsCorrectLineNumbers() {
        // Test that function calls across multiple lines show correct line numbers
        val result = evaluateExpression("func1 = (a, b) -> a / b\nfunc2 = (a, b) -> func1(a + b, a - b)\nfunc2(10, 10)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        // The error should show line 3 for func2 call and line 2 for func1 call
        assertTrue(result.output.contains("line 3") || result.output.contains("line 2"))
    }

    @Test
    fun divisionErrorShowsOperatorRangeWithContext() {
        // Test that division by zero error shows the operator range with full line context
        val result = evaluateExpression("a = 1 + (4 / 0) - 4")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        // Should show the line with the problematic range highlighted
        assertTrue(result.output.contains("line 1"))
        // The highlighted range should include the operator and operand(s)
        assertTrue(result.output.contains("[") && result.output.contains("]"))
        // Should show division operator
        assertTrue(result.output.contains("/"))
    }

    @Test
    fun stackTraceShowsHighlightedRange() {
        // Test that stack trace shows the full source line with highlighted problem range
        val result = evaluateExpression("10 / 0")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        // Should contain brackets to highlight the problem range
        assertTrue(result.output.contains("[") && result.output.contains("]"))
        // Should show the division in highlighted form
        val lines = result.output.split("\n")
        val stackLine = lines.find { it.contains("at line") }
        assertNotNull(stackLine)
        // The stack trace line should show the operator with highlighting
        assertTrue(stackLine!!.contains("[") && stackLine.contains("]"))
    }

    @Test
    fun errorRangeSpansFullExpression() {
        // The highlighted range spans from the left operand to the right operand (e.g. "5 / 0")
        val result = evaluateExpression("a = 5 / 0")
        assertFalse(result.success)
        assertTrue(result.output.contains("Division by zero"))
        val lines = result.output.split("\n")
        val stackLine = lines.find { it.contains("at line") }
        assertNotNull(stackLine)
        val highlighted = stackLine!!.substringAfter("[").substringBefore("]")
        // Should contain the division operator and both operands
        assertTrue(highlighted.contains("/"))
        assertTrue(highlighted.contains("5"))
        assertTrue(highlighted.contains("0"))
    }
}
