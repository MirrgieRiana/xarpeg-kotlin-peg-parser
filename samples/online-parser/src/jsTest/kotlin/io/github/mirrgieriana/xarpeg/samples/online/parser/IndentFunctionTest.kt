package io.github.mirrgieriana.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndentFunctionTest {

    // -- OnlineParserParseContext --

    @Test
    fun parseContextInitiallyHasZeroIndent() {
        assertEquals(0, OnlineParserParseContext("source").currentIndent)
    }

    @Test
    fun parseContextPushAndPopIndent() {
        val ctx = OnlineParserParseContext("source")
        ctx.pushIndent(4)
        assertEquals(4, ctx.currentIndent)
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

    // -- Basic indent function definitions --

    @Test
    fun indentFunctionDef() {
        assertEquals("42", evaluateExpression("f():\n    42\nf()").output)
        assertEquals("10", evaluateExpression("double(x):\n    x * 2\ndouble(5)").output)
        assertEquals("7", evaluateExpression("add(a, b):\n    a + b\nadd(3, 4)").output)
        assertEquals("6", evaluateExpression("sumThree(a, b, c):\n    a + b + c\nsumThree(1, 2, 3)").output)
        assertEquals("30", evaluateExpression("add( a , b ):\n    a + b\nadd(10, 20)").output)
    }

    // -- Indent level variants --

    @Test
    fun indentLevelVariants() {
        assertEquals("14", evaluateExpression("f(x):\n x * 2\nf(7)").output)
        assertEquals("12", evaluateExpression("f(x):\n  x * 3\nf(4)").output)
        assertEquals("12", evaluateExpression("f(x):\n    x * 3\nf(4)").output)
        assertEquals("12", evaluateExpression("f(x):\n        x * 3\nf(4)").output)
        assertEquals("12", evaluateExpression("f(x):\n\tx * 3\nf(4)").output)
    }

    // -- Multi-line body --

    @Test
    fun multiLineBodyContinuation() {
        assertEquals("15", evaluateExpression("add(a, b):\n    a +\n    b\nadd(10, 5)").output)
        assertEquals("6", evaluateExpression("sub(a, b):\n    a -\n    b\nsub(10, 4)").output)
        assertEquals("42", evaluateExpression("mul(a, b):\n    a *\n    b\nmul(6, 7)").output)
        assertEquals("6", evaluateExpression("sum3(a, b, c):\n    a +\n    b +\n    c\nsum3(1, 2, 3)").output)
    }

    @Test
    fun multiLineBodyWithPrecedence() {
        // a + b * c: operator precedence means b * c evaluates first
        assertEquals("14", evaluateExpression("calc(a, b, c):\n    a +\n    b *\n    c\ncalc(2, 3, 4)").output)
    }

    @Test
    fun multiLineBodyWithTernary() {
        assertEquals("7", evaluateExpression("check(a, b):\n    a > b ? a :\n    b\ncheck(7, 3)").output)
        assertEquals("3", evaluateExpression("minOf(a, b):\n    a < b ? a :\n    b\nminOf(7, 3)").output)
    }

    // -- Recursive indent functions --

    @Test
    fun recursion() {
        assertEquals("120", evaluateExpression("factorial(n):\n    n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(5)").output)
        assertEquals("1", evaluateExpression("factorial(n):\n    n <= 1 ? 1 : n * factorial(n - 1)\nfactorial(0)").output)
        assertEquals("13", evaluateExpression("fib(n):\n    n <= 1 ? n : fib(n - 1) + fib(n - 2)\nfib(7)").output)
        assertEquals("15", evaluateExpression("sum(n, acc):\n    n <= 0 ? acc : sum(n - 1, acc + n)\nsum(5, 0)").output)
    }

    @Test
    fun recursionWithMultiLineBody() {
        assertEquals("120", evaluateExpression("factorial(n):\n    n <= 1 ? 1 :\n    n * factorial(n - 1)\nfactorial(5)").output)
    }

    @Test
    fun multiStatementBody() {
        assertEquals("11", evaluateExpression("f(x):\n    var y = x * 2\n    y + 1\nf(5)").output)
    }

    // -- Multiple functions and mixing --

    @Test
    fun multipleIndentFunctions() {
        assertEquals("14", evaluateExpression("double(x):\n    x * 2\ntriple(x):\n    x * 3\ndouble(4) + triple(2)").output)
        assertEquals("12", evaluateExpression("double(x):\n    x * 2\nquadruple(x):\n    double(double(x))\nquadruple(3)").output)
        assertEquals("24", evaluateExpression("triple(x):\n    x * 3\ndoubleThenTriple(x):\n    triple(x * 2)\ndoubleThenTriple(4)").output)
    }

    @Test
    fun mixedWithVariableAndLambda() {
        assertEquals("15", evaluateExpression("var base = 10\naddBase(x):\n    x + base\naddBase(5)").output)
        assertEquals("27", evaluateExpression("var square = (x) -> x * x\ncube(x):\n    x * square(x)\ncube(3)").output)
        assertEquals("20", evaluateExpression("f(x):\n    x + 1\nf(9) * 2").output)
    }

    // -- Indent boundary errors --

    @Test
    fun outdentedOperandFails() {
        assertFalse(evaluateExpression("calc(n):\n    n +\n  1\ncalc(5)").success)
        assertFalse(evaluateExpression("f(n):\n    n > 0 ? n :\n  0\nf(5)").success)
        assertFalse(evaluateExpression("f():\n1\nf()").success)
    }

    @Test
    fun indentBoundarySucceeds() {
        assertEquals("6", evaluateExpression("calc(n):\n    n +\n    1\ncalc(5)").output)
        assertEquals("6", evaluateExpression("f(n):\n  n +\n  1\nf(5)").output)
        assertEquals("6", evaluateExpression("f(n):\n    n +\n        1\nf(5)").output)
    }

    @Test
    fun outdentedLineBecomesNewStatement() {
        assertEquals("5", evaluateExpression("f():\n    42\n5").output)
    }

    // -- Newline variants --

    @Test
    fun crlfNewlines() {
        assertEquals("10", evaluateExpression("f(x):\r\n    x + 1\r\nf(9)").output)
        assertEquals("10", evaluateExpression("add(a, b):\r\n    a +\r\n    b\r\nadd(3, 7)").output)
        assertEquals("24", evaluateExpression("factorial(n):\r\n    n <= 1 ? 1 : n * factorial(n - 1)\r\nfactorial(4)").output)
    }

    @Test
    fun bareCrNewline() {
        assertEquals("10", evaluateExpression("add(a, b):\n    a +\r    b\nadd(3, 7)").output)
    }

    // -- Edge cases --

    @Test
    fun returnsBoolean() {
        assertEquals("true", evaluateExpression("isPositive(n):\n    n > 0\nisPositive(5)").output)
        assertEquals("false", evaluateExpression("isPositive(n):\n    n > 0\nisPositive(0 - 5)").output)
        assertEquals("true", evaluateExpression("cmp(a, b):\n    a == b\ncmp(3, 3)").output)
    }

    @Test
    fun decimalArithmetic() {
        assertEquals("2.5", evaluateExpression("half(x):\n    x / 2\nhalf(5.0)").output)
    }

    @Test
    fun functionCallLimitExceeded() {
        val result = evaluateExpression("inf(n):\n    inf(n + 1)\ninf(0)")
        assertFalse(result.success)
        assertTrue(result.output.contains("Maximum function call limit"))
    }
}
