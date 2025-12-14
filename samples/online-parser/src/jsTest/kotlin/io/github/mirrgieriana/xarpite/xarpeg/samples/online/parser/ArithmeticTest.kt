package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ArithmeticTest {

    @Test
    fun parsesExpressionWithWhitespaceAroundPlus() {
        assertEquals("3", parseExpression("1 + 2"))
    }

    @Test
    fun parsesExpressionWithWhitespaceAroundStar() {
        assertEquals("14", parseExpression("2 * ( 3 + 4 )"))
    }

    @Test
    fun parsesDecimalNumber() {
        assertEquals("3.14", parseExpression("3.14"))
    }

    @Test
    fun parsesDecimalNumberInExpression() {
        assertEquals("5.5", parseExpression("2.5 + 3.0"))
    }

    @Test
    fun parsesComplexArithmeticExpression() {
        assertEquals("23", parseExpression("(5 + 3) * 2 + 7"))
    }

    @Test
    fun parsesNestedParentheses() {
        assertEquals("14", parseExpression("((2 + 3) * 2) + 4"))
    }

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
}
