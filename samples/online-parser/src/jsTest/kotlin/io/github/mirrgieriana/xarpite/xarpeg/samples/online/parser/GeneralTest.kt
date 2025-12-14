package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneralTest {

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
    fun parsesIdentifierWithUnderscore() {
        assertEquals("42", parseExpression("my_var = 42"))
    }

    @Test
    fun parsesIdentifierWithNumbers() {
        assertEquals("100", parseExpression("var123 = 100"))
    }

    @Test
    fun parsesDecimalNumberInVariableAssignment() {
        assertEquals("1.5", parseExpression("pi_half = 1.5"))
    }

    @Test
    fun parsesVariableInArithmeticExpression() {
        assertEquals("15", parseExpression("x = 5 + 10"))
    }

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

    @Test
    fun parsesMultipleVariableAssignments() {
        // Only the last assignment value is returned
        assertEquals("20", parseExpression("x = 20"))
    }
}
