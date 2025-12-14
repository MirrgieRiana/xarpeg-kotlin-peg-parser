package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ArithmeticTest {

    @Test
    fun parsesExpressionWithWhitespaceAroundStar() {
        assertEquals("14", parseExpression("2 * ( 3 + 4 )"))
    }

    @Test
    fun parsesComplexArithmeticExpression() {
        assertEquals("14", parseExpression("2 + 3 * 4"))
    }

    @Test
    fun parsesVariableInArithmeticExpression() {
        assertEquals("15", parseExpression("x = 5\ny = 10\nx + y"))
    }

    @Test
    fun parsesNestedParentheses() {
        assertEquals("9", parseExpression("((1 + 2) * 3)"))
    }

    @Test
    fun respectsOperatorPrecedenceMultiplicationFirst() {
        assertEquals("14", parseExpression("2 + 3 * 4"))
    }

    @Test
    fun respectsOperatorPrecedenceDivisionFirst() {
        assertEquals("5", parseExpression("10 / 2 + 3"))
    }

    @Test
    fun respectsOperatorPrecedenceWithSubtraction() {
        assertEquals("11", parseExpression("20 - 3 * 3"))
    }

    @Test
    fun parsesComplexArithmeticWithVariables() {
        assertEquals("42", parseExpression("a = 10\nb = 3\nc = 2\na * b + c * 6"))
    }
}
