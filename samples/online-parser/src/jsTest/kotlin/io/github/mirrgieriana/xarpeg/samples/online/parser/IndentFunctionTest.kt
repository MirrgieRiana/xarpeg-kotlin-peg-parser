package io.github.mirrgieriana.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndentFunctionTest {

    // -----------------------------------------------------------------------
    // OnlineParserParseContext unit tests
    // -----------------------------------------------------------------------

    @Test
    fun parseContextInitiallyHasZeroIndent() {
        val ctx = OnlineParserParseContext("source")
        assertEquals(0, ctx.currentIndent)
    }

    @Test
    fun parseContextPushIndentUpdatesCurrentIndent() {
        val ctx = OnlineParserParseContext("source")
        ctx.pushIndent(4)
        assertEquals(4, ctx.currentIndent)
    }

    @Test
    fun parseContextPopIndentRestoresCurrentIndent() {
        val ctx = OnlineParserParseContext("source")
        ctx.pushIndent(4)
        ctx.popIndent()
        assertEquals(0, ctx.currentIndent)
    }

    @Test
    fun parseContextStacksMultipleIndentLevels() {
        val ctx = OnlineParserParseContext("source")
        ctx.pushIndent(4)
        ctx.pushIndent(8)
        assertEquals(8, ctx.currentIndent)
        ctx.popIndent()
        assertEquals(4, ctx.currentIndent)
        ctx.popIndent()
        assertEquals(0, ctx.currentIndent)
    }

    // -----------------------------------------------------------------------
    // Basic indent function definitions
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionDefNoParams() {
        val result = evaluateExpression("f():\n    42\nf()")
        assertTrue(result.success)
        assertEquals("42", result.output)
    }

    @Test
    fun indentFunctionDefOneParam() {
        val result = evaluateExpression("double(x):\n    x * 2\ndouble(5)")
        assertTrue(result.success)
        assertEquals("10", result.output)
    }

    @Test
    fun indentFunctionDefTwoParams() {
        val result = evaluateExpression("add(a, b):\n    a + b\nadd(3, 4)")
        assertTrue(result.success)
        assertEquals("7", result.output)
    }

    @Test
    fun indentFunctionDefThreeParams() {
        val result = evaluateExpression("sumThree(a, b, c):\n    a + b + c\nsumThree(1, 2, 3)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    @Test
    fun indentFunctionDefProducesLambdaOutput() {
        val result = evaluateExpression("f(x):\n    x + 1")
        assertTrue(result.success)
        assertTrue(result.output.contains("lambda"))
    }

    @Test
    fun indentFunctionDefResultIsCallable() {
        val result = evaluateExpression("inc(x):\n    x + 1\ninc(9)")
        assertTrue(result.success)
        assertEquals("10", result.output)
    }

    @Test
    fun indentFunctionDefWithParamListWhitespace() {
        val result = evaluateExpression("add( a , b ):\n    a + b\nadd(10, 20)")
        assertTrue(result.success)
        assertEquals("30", result.output)
    }

    // -----------------------------------------------------------------------
    // Indent level variants
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionDefWith2SpaceIndent() {
        val result = evaluateExpression("f(x):\n  x * 3\nf(4)")
        assertTrue(result.success)
        assertEquals("12", result.output)
    }

    @Test
    fun indentFunctionDefWith4SpaceIndent() {
        val result = evaluateExpression("f(x):\n    x * 3\nf(4)")
        assertTrue(result.success)
        assertEquals("12", result.output)
    }

    @Test
    fun indentFunctionDefWith8SpaceIndent() {
        val result = evaluateExpression("f(x):\n        x * 3\nf(4)")
        assertTrue(result.success)
        assertEquals("12", result.output)
    }

    @Test
    fun indentFunctionDefWithTabIndent() {
        val result = evaluateExpression("f(x):\n\tx * 3\nf(4)")
        assertTrue(result.success)
        assertEquals("12", result.output)
    }

    @Test
    fun indentFunctionDefWith1SpaceIndent() {
        val result = evaluateExpression("f(x):\n x * 2\nf(7)")
        assertTrue(result.success)
        assertEquals("14", result.output)
    }

    // -----------------------------------------------------------------------
    // Multi-line body expressions (continuation across lines)
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionBodyAdditionContinuedOnNextLine() {
        val result = evaluateExpression("add(a, b):\n    a +\n    b\nadd(10, 5)")
        assertTrue(result.success)
        assertEquals("15", result.output)
    }

    @Test
    fun indentFunctionBodySubtractionContinuedOnNextLine() {
        val result = evaluateExpression("sub(a, b):\n    a -\n    b\nsub(10, 4)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    @Test
    fun indentFunctionBodyMultiplicationContinuedOnNextLine() {
        val result = evaluateExpression("mul(a, b):\n    a *\n    b\nmul(6, 7)")
        assertTrue(result.success)
        assertEquals("42", result.output)
    }

    @Test
    fun indentFunctionBodyChainedOperatorsContinuation() {
        // a + b * c: operator precedence means b * c evaluates first
        val result = evaluateExpression("calc(a, b, c):\n    a +\n    b *\n    c\ncalc(2, 3, 4)")
        assertTrue(result.success)
        assertEquals("14", result.output)  // 2 + (3 * 4) = 14
    }

    @Test
    fun indentFunctionBodyTernaryAcrossLines() {
        val result = evaluateExpression("check(a, b):\n    a > b ? a :\n    b\ncheck(7, 3)")
        assertTrue(result.success)
        assertEquals("7", result.output)
    }

    @Test
    fun indentFunctionBodyTernaryFalseBranchAcrossLines() {
        val result = evaluateExpression("minOf(a, b):\n    a < b ? a :\n    b\nminOf(7, 3)")
        assertTrue(result.success)
        assertEquals("3", result.output)
    }

    @Test
    fun indentFunctionBodyThreeLineExpression() {
        // Each part of addition on its own line
        val result = evaluateExpression("sum3(a, b, c):\n    a +\n    b +\n    c\nsum3(1, 2, 3)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    // -----------------------------------------------------------------------
    // Recursive indent functions
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionRecursiveFactorial() {
        val result = evaluateExpression(
            "factorial(n):\n    n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(5)"
        )
        assertTrue(result.success)
        assertEquals("120", result.output)
    }

    @Test
    fun indentFunctionRecursiveFactorialMultiLineBody() {
        val result = evaluateExpression(
            "factorial(n):\n    n <= 1 ? 1 :\n    n * factorial(n - 1)\nfactorial(5)"
        )
        assertTrue(result.success)
        assertEquals("120", result.output)
    }

    @Test
    fun indentFunctionRecursiveFactorialBase() {
        val result = evaluateExpression(
            "factorial(n):\n    n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(0)"
        )
        assertTrue(result.success)
        assertEquals("1", result.output)
    }

    @Test
    fun indentFunctionRecursiveFibonacci() {
        val result = evaluateExpression(
            "fib(n):\n    n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)"
        )
        assertTrue(result.success)
        assertEquals("13", result.output)
    }

    @Test
    fun indentFunctionRecursiveSumWithAccumulator() {
        val result = evaluateExpression(
            "sum(n, acc):\n    n <= 0 ? acc : sum(n - 1, acc + n)\nsum(5, 0)"
        )
        assertTrue(result.success)
        assertEquals("15", result.output)  // 1+2+3+4+5 = 15
    }

    // -----------------------------------------------------------------------
    // Multiple indent functions and mixing with regular assignment
    // -----------------------------------------------------------------------

    @Test
    fun multipleIndentFunctionDefs() {
        val input = "double(x):\n    x * 2\ntriple(x):\n    x * 3\ndouble(4) + triple(2)"
        val result = evaluateExpression(input)
        assertTrue(result.success)
        assertEquals("14", result.output)  // 8 + 6 = 14
    }

    @Test
    fun indentFunctionMixedWithRegularVariableAssignment() {
        val input = "var base = 10\naddBase(x):\n    x + base\naddBase(5)"
        val result = evaluateExpression(input)
        assertTrue(result.success)
        assertEquals("15", result.output)
    }

    @Test
    fun indentFunctionCallingRegularLambda() {
        val input = "var square = (x) -> x * x\ncube(x):\n    x * square(x)\ncube(3)"
        val result = evaluateExpression(input)
        assertTrue(result.success)
        assertEquals("27", result.output)
    }

    @Test
    fun indentFunctionCalledByAnotherIndentFunction() {
        val input = "double(x):\n    x * 2\nquadruple(x):\n    double(double(x))\nquadruple(3)"
        val result = evaluateExpression(input)
        assertTrue(result.success)
        assertEquals("12", result.output)
    }

    @Test
    fun indentFunctionResultUsedInExpression() {
        val input = "f(x):\n    x + 1\nf(9) * 2"
        val result = evaluateExpression(input)
        assertTrue(result.success)
        assertEquals("20", result.output)  // (9+1) * 2 = 20
    }

    // -----------------------------------------------------------------------
    // Error cases: outdented operands (the key issue scenario)
    // -----------------------------------------------------------------------

    @Test
    fun issueScenarioOutdentedOperandAfterOperatorFails() {
        // The key scenario from the issue:
        // `n +` on properly indented line, `1` at less-indented column → should fail
        val result = evaluateExpression("calc(n):\n    n +\n  1\ncalc(5)")
        assertFalse(result.success)
    }

    @Test
    fun outdentedTernaryFalseBranchFails() {
        // Ternary false branch at insufficient indent → should fail
        val result = evaluateExpression("f(n):\n    n > 0 ? n :\n  0\nf(5)")
        assertFalse(result.success)
    }

    @Test
    fun zeroIndentBodyIsNotAnIndentBlock() {
        // Body at column 0 (indent 0 = current indent 0) → not accepted as indent block
        val result = evaluateExpression("f():\n1\nf()")
        assertFalse(result.success)
    }

    @Test
    fun bodyWithCorrectIndentSucceeds() {
        // Positive counterpart: same structure but with sufficient indent
        val result = evaluateExpression("calc(n):\n    n +\n    1\ncalc(5)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    @Test
    fun bodyExactlyAtRequiredIndentSucceeds() {
        // Continuation line at exactly the required indent level succeeds
        val result = evaluateExpression("f(n):\n  n +\n  1\nf(5)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    @Test
    fun bodyWithExtraIndentBeyondRequiredSucceeds() {
        // Continuation line with more than required indent also succeeds
        val result = evaluateExpression("f(n):\n    n +\n        1\nf(5)")
        assertTrue(result.success)
        assertEquals("6", result.output)
    }

    @Test
    fun outdentedLineBecomesNewProgramStatement() {
        // A line with less indentation than the body ends the function body
        // and becomes a new top-level statement
        val result = evaluateExpression("f():\n    42\n5")
        assertTrue(result.success)
        assertEquals("5", result.output)
    }

    // -----------------------------------------------------------------------
    // Newline variant tests: CRLF (\r\n) and bare CR (\r)
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionWithCRLFInFunctionDefinition() {
        val result = evaluateExpression("f(x):\r\n    x + 1\r\nf(9)")
        assertTrue(result.success)
        assertEquals("10", result.output)
    }

    @Test
    fun indentFunctionBodyContinuationWithCRLF() {
        val result = evaluateExpression("add(a, b):\r\n    a +\r\n    b\r\nadd(3, 7)")
        assertTrue(result.success)
        assertEquals("10", result.output)
    }

    @Test
    fun indentFunctionBodyContinuationWithBareCR() {
        // bare \r as newline within multi-line body continuation
        val result = evaluateExpression("add(a, b):\n    a +\r    b\nadd(3, 7)")
        assertTrue(result.success)
        assertEquals("10", result.output)
    }

    @Test
    fun indentFunctionWithCRLFMultiLineBody() {
        val result = evaluateExpression(
            "factorial(n):\r\n    n <= 1 ? 1 : n * factorial(n - 1)\r\nfactorial(4)"
        )
        assertTrue(result.success)
        assertEquals("24", result.output)
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    fun indentFunctionBodyReturnsBoolean() {
        val result = evaluateExpression("isPositive(n):\n    n > 0\nisPositive(5)")
        assertTrue(result.success)
        assertEquals("true", result.output)
    }

    @Test
    fun indentFunctionBodyReturnsBooleanFalse() {
        // Grammar doesn't support negative literals directly in args; use subtraction
        val result = evaluateExpression("isPositive(n):\n    n > 0\nisPositive(0 - 5)")
        assertTrue(result.success)
        assertEquals("false", result.output)
    }

    @Test
    fun indentFunctionBodyWithComparisonResult() {
        val result = evaluateExpression("cmp(a, b):\n    a == b\ncmp(3, 3)")
        assertTrue(result.success)
        assertEquals("true", result.output)
    }

    @Test
    fun indentFunctionWithDecimalArithmetic() {
        val result = evaluateExpression("half(x):\n    x / 2\nhalf(5.0)")
        assertTrue(result.success)
        assertEquals("2.5", result.output)
    }

    @Test
    fun indentFunctionDefinedThenCalledImmediately() {
        // Inline: define and call in one program
        val result = evaluateExpression("sq(x):\n    x * x\nsq(7)")
        assertTrue(result.success)
        assertEquals("49", result.output)
    }

    @Test
    fun indentFunctionBodyWithNestedFunctionCall() {
        val result = evaluateExpression(
            "triple(x):\n    x * 3\ndoubleThenTriple(x):\n    triple(x * 2)\ndoubleThenTriple(4)"
        )
        assertTrue(result.success)
        assertEquals("24", result.output)  // triple(4 * 2) = triple(8) = 24
    }

    @Test
    fun indentFunctionCallLimitExceeded() {
        val result = evaluateExpression("inf(n):\n    inf(n + 1)\ninf(0)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Maximum function call limit"))
    }
}
