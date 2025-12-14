package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ComparisonTest {

    @Test
    fun parsesLessThanComparison() {
        assertEquals("true", parseExpression("3 < 5"))
    }

    @Test
    fun parsesGreaterThanComparison() {
        assertEquals("false", parseExpression("3 > 5"))
    }

    @Test
    fun parsesLessThanOrEqualComparison() {
        assertEquals("true", parseExpression("3 <= 5"))
    }

    @Test
    fun parsesGreaterThanOrEqualComparison() {
        assertEquals("false", parseExpression("3 >= 5"))
    }

    @Test
    fun parsesEqualityComparison() {
        assertEquals("true", parseExpression("5 == 5"))
    }

    @Test
    fun parsesInequalityComparison() {
        assertEquals("true", parseExpression("3 != 5"))
    }

    @Test
    fun parsesTernaryOperator() {
        assertEquals("10", parseExpression("5 > 3 ? 10 : 20"))
    }

    @Test
    fun parsesTernaryWithFalseBranch() {
        assertEquals("20", parseExpression("3 > 5 ? 10 : 20"))
    }

    @Test
    fun parsesNestedTernary() {
        assertEquals("2", parseExpression("5 > 3 ? (2 > 1 ? 2 : 3) : 4"))
    }

    @Test
    fun parsesBooleanTrue() {
        assertEquals("true", parseExpression("true"))
    }

    @Test
    fun parsesBooleanFalse() {
        assertEquals("false", parseExpression("false"))
    }

    @Test
    fun parsesBooleanEquality() {
        assertEquals("true", parseExpression("true == true"))
    }

    @Test
    fun parsesBooleanInequality() {
        assertEquals("true", parseExpression("true != false"))
    }

    @Test
    fun parsesComparisonWithVariables() {
        assertEquals("true", parseExpression("x = 10\ny = 5\nx > y"))
    }

    @Test
    fun parsesTernaryWithVariables() {
        assertEquals("greater", parseExpression("x = 10\ny = 5\nx > y ? \"greater\" : \"less\""))
    }
}
