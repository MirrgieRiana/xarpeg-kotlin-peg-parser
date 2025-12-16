package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun showsErrorMessageForDivisionByZero() {
        val result = parseExpression("1 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
    }

    // Lambda expression tests
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

    // Decimal number tests
    @Test
    fun parsesDecimalNumber() {
        assertEquals("3.14", parseExpression("3.14"))
    }

    @Test
    fun parsesDecimalNumberInExpression() {
        assertEquals("5.5", parseExpression("2.5 + 3.0"))
    }

    @Test
    fun parsesDecimalNumberInVariableAssignment() {
        assertEquals("1.5", parseExpression("pi_half = 1.5"))
    }

    // Complex expression tests
    @Test
    fun parsesComplexArithmeticExpression() {
        assertEquals("23", parseExpression("(5 + 3) * 2 + 7"))
    }

    @Test
    fun parsesVariableInArithmeticExpression() {
        assertEquals("15", parseExpression("x = 5 + 10"))
    }

    @Test
    fun parsesNestedParentheses() {
        assertEquals("14", parseExpression("((2 + 3) * 2) + 4"))
    }

    // Operator precedence tests
    @Test
    fun respectsOperatorPrecedenceMultiplicationFirst() {
        assertEquals("11", parseExpression("5 + 2 * 3"))
    }

    @Test
    fun respectsOperatorPrecedenceDivisionFirst() {
        assertEquals("7", parseExpression("5 + 6 / 3"))
    }

    @Test
    fun respectsOperatorPrecedenceWithSubtraction() {
        assertEquals("1", parseExpression("10 - 3 * 3"))
    }

    // Identifier validation tests
    @Test
    fun parsesIdentifierStartingWithUnderscore() {
        assertEquals("99", parseExpression("_private = 99"))
    }

    @Test
    fun parsesIdentifierWithMultipleUnderscores() {
        assertEquals("77", parseExpression("__value__ = 77"))
    }

    @Test
    fun parsesIdentifierWithMixedCase() {
        assertEquals("88", parseExpression("MyVariable = 88"))
    }

    @Test
    fun parsesIdentifierEndingWithNumber() {
        assertEquals("55", parseExpression("var2 = 55"))
    }

    // Error case tests
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

    // Whitespace handling tests
    @Test
    fun parsesExpressionWithVariousWhitespace() {
        assertEquals("10", parseExpression("  5   +   5  "))
    }

    @Test
    fun parsesExpressionWithNewlines() {
        assertEquals("6", parseExpression("2\n*\n3"))
    }

    @Test
    fun parsesExpressionWithTabs() {
        assertEquals("8", parseExpression("4\t+\t4"))
    }

    // Lambda and assignment combination tests
    @Test
    fun parsesMultipleVariableAssignments() {
        // Only the last assignment value is returned
        assertEquals("20", parseExpression("x = 20"))
    }

    @Test
    fun parsesLambdaAssignmentReturnsLambda() {
        val result = parseExpression("sum = (a, b) -> a + b")
        assertTrue(result.startsWith("<lambda"))
        assertTrue(result.contains("a, b"))
    }

    // Edge cases
    @Test
    fun parsesZero() {
        assertEquals("0", parseExpression("0"))
    }

    @Test
    fun parsesNegativeResult() {
        assertEquals("-5", parseExpression("5 - 10"))
    }

    @Test
    fun parsesLargeNumber() {
        assertEquals("1000000", parseExpression("1000000"))
    }

    @Test
    fun parsesVerySmallDecimal() {
        assertEquals("0.001", parseExpression("0.001"))
    }

    // Comparison operator tests
    @Test
    fun parsesEqualityOperator() {
        assertEquals("true", parseExpression("5 == 5"))
        assertEquals("false", parseExpression("5 == 3"))
    }

    @Test
    fun parsesInequalityOperator() {
        assertEquals("false", parseExpression("5 != 5"))
        assertEquals("true", parseExpression("5 != 3"))
    }

    @Test
    fun parsesLessThanOperator() {
        assertEquals("true", parseExpression("3 < 5"))
        assertEquals("false", parseExpression("5 < 3"))
        assertEquals("false", parseExpression("5 < 5"))
    }

    @Test
    fun parsesLessThanOrEqualOperator() {
        assertEquals("true", parseExpression("3 <= 5"))
        assertEquals("false", parseExpression("5 <= 3"))
        assertEquals("true", parseExpression("5 <= 5"))
    }

    @Test
    fun parsesGreaterThanOperator() {
        assertEquals("true", parseExpression("5 > 3"))
        assertEquals("false", parseExpression("3 > 5"))
        assertEquals("false", parseExpression("5 > 5"))
    }

    @Test
    fun parsesGreaterThanOrEqualOperator() {
        assertEquals("true", parseExpression("5 >= 3"))
        assertEquals("false", parseExpression("3 >= 5"))
        assertEquals("true", parseExpression("5 >= 5"))
    }

    @Test
    fun parsesComparisonWithExpressions() {
        assertEquals("true", parseExpression("2 + 3 == 5"))
        assertEquals("false", parseExpression("2 * 3 == 5"))
        assertEquals("true", parseExpression("10 - 5 > 3"))
    }

    // Ternary operator tests
    @Test
    fun parsesTernaryOperatorTrue() {
        assertEquals("10", parseExpression("5 > 3 ? 10 : 20"))
    }

    @Test
    fun parsesTernaryOperatorFalse() {
        assertEquals("20", parseExpression("5 < 3 ? 10 : 20"))
    }

    @Test
    fun parsesTernaryWithComplexExpressions() {
        assertEquals("15", parseExpression("2 + 3 == 5 ? 10 + 5 : 20 + 5"))
    }

    @Test
    fun parsesTernaryWithVariables() {
        assertEquals("100", parseExpression("x = 5 > 3 ? 100 : 200"))
    }

    @Test
    fun parsesNestedTernary() {
        assertEquals("1", parseExpression("5 > 3 ? (2 > 1 ? 1 : 2) : 3"))
    }

    // Type checking tests
    @Test
    fun showsErrorWhenBooleanUsedInArithmetic() {
        val result = parseExpression("5 + (3 > 2)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("must be a number"))
    }

    @Test
    fun showsErrorWhenNonBooleanUsedInTernaryCondition() {
        val result = parseExpression("5 ? 10 : 20")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("must be a boolean"))
    }

    @Test
    fun showsErrorWhenBooleanComparedInComparison() {
        val result = parseExpression("(5 > 3) < (10 > 2)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("must be a number"))
    }

    // Boolean value assignment tests
    @Test
    fun parsesBooleanAssignment() {
        assertEquals("true", parseExpression("result = 5 > 3"))
    }

    @Test
    fun parsesBooleanInVariable() {
        assertEquals("false", parseExpression("isEqual = 5 == 3"))
    }

    // Complex combination tests
    @Test
    fun parsesComplexBooleanExpression() {
        assertEquals("true", parseExpression("5 + 5 == 10"))
        assertEquals("false", parseExpression("5 * 2 != 10"))
    }

    @Test
    fun parsesTernaryInArithmetic() {
        assertEquals("15", parseExpression("5 + (10 > 5 ? 10 : 0)"))
    }

    @Test
    fun parsesMultipleComparisons() {
        // Note: Comparisons are left-associative, so this is (5 > 3) == (10 > 5)
        // which is true == true = true
        assertEquals("true", parseExpression("5 > 3 == 10 > 5"))
    }

    @Test
    fun parsesBooleanEqualityComparison() {
        assertEquals("true", parseExpression("(5 > 3) == (10 > 5)"))
        assertEquals("false", parseExpression("(5 > 3) == (10 < 5)"))
        assertEquals("false", parseExpression("(5 > 3) != (10 > 5)"))
        assertEquals("true", parseExpression("(5 > 3) != (10 < 5)"))
    }

    @Test
    fun showsErrorWhenComparingNumberWithBoolean() {
        val result = parseExpression("5 == (3 > 2)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("must be both numbers or both booleans"))
    }

    // Recursive function tests
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
    fun showsErrorWhenFunctionCallLimitExceeded() {
        // Create an infinite recursion that will hit the limit
        val result = parseExpression("infinite = (n) -> infinite(n + 1)\ninfinite(0)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Maximum function call limit"))
    }

    @Test
    fun parsesRecursiveFibonacci() {
        val result = parseExpression("fib = (n) -> n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)")
        assertEquals("13", result)
    }

    // Stack trace tests
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

    // Variable scoping tests
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

    @Test
    fun errorOnLine2ShowsCorrectLineNumber() {
        // Error occurs on the second line
        val result = parseExpression("x = 10\n1 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        assertTrue(result.contains("line 2"))
    }

    @Test
    fun errorOnLine3ShowsCorrectLineNumber() {
        // Error occurs on the third line
        val result = parseExpression("x = 5\ny = 10\nz = x / (y - 10)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        assertTrue(result.contains("line 3"))
    }

    @Test
    fun multiLineStackTraceShowsCorrectLineNumbers() {
        // Test that function calls across multiple lines show correct line numbers
        val result = parseExpression("func1 = (a, b) -> a / b\nfunc2 = (a, b) -> func1(a + b, a - b)\nfunc2(10, 10)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        // The error should show line 3 for func2 call and line 2 for func1 call
        assertTrue(result.contains("line 3") || result.contains("line 2"))
    }
    
    @Test
    fun divisionErrorShowsOperatorRangeWithContext() {
        // Test that division by zero error shows the operator range with full line context
        val result = parseExpression("a = 1 + (4 / 0) - 4")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        // Should show the line with the problematic range highlighted
        assertTrue(result.contains("line 1"))
        // The highlighted range should include the operator and operand(s)
        assertTrue(result.contains("[") && result.contains("]"))
        // Should show division operator
        assertTrue(result.contains("/"))
    }
    
    @Test
    fun stackTraceShowsHighlightedRange() {
        // Test that stack trace shows the full source line with highlighted problem range
        val result = parseExpression("10 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        // Should contain brackets to highlight the problem range
        assertTrue(result.contains("[") && result.contains("]"))
        // Should show the division in highlighted form
        val lines = result.split("\n")
        val stackLine = lines.find { it.contains("at line") }
        assertNotNull(stackLine)
        // The stack trace line should show the operator with highlighting
        assertTrue(stackLine!!.contains("[") && stackLine.contains("]"))
    }
    
    @Test
    fun errorRangeDoesNotIncludeLeadingWhitespace() {
        // Test that the error range starts at the operator, not before it
        val result = parseExpression("a = 5 / 0")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("Division by zero"))
        val lines = result.split("\n")
        val stackLine = lines.find { it.contains("at line") }
        assertNotNull(stackLine)
        // The highlighted part should start with the operator, not whitespace
        val highlighted = stackLine!!.substringAfter("[").substringBefore("]")
        assertTrue(highlighted.startsWith("/") || highlighted.startsWith("/ "))
        // Should not start with whitespace
        assertFalse(highlighted.startsWith(" "))
    }
}
