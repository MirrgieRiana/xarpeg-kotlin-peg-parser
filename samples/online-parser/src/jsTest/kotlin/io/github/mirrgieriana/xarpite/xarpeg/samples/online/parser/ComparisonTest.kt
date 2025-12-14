package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComparisonTest {

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

    @Test
    fun parsesBooleanAssignment() {
        assertEquals("true", parseExpression("result = 5 > 3"))
    }

    @Test
    fun parsesBooleanInVariable() {
        assertEquals("false", parseExpression("isEqual = 5 == 3"))
    }

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

    @Test
    fun showsErrorWhenComparingNumberWithBoolean() {
        val result = parseExpression("5 == (3 > 2)")
        assertTrue(result.startsWith("Error"))
        assertTrue(result.contains("must be both numbers or both booleans"))
    }
}
