package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
import io.github.mirrgieriana.xarpeg.samples.online.parser.requireNumber

/**
 * Base class for ordering comparison operators (<, <=, >, >=).
 */
abstract class ComparisonExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>,
) : Expression {
    abstract val operatorSymbol: String
    abstract fun compare(leftValue: Double, rightValue: Double): Boolean

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)
        val opCtx = ctx.pushFrame("$operatorSymbol operator", position)
        val leftNum = leftVal.requireNumber(opCtx, operatorSymbol, "Left")
        val rightNum = rightVal.requireNumber(opCtx, operatorSymbol, "Right")
        return BooleanValue(compare(leftNum, rightNum))
    }
}

/**
 * Less-than expression (`left < right`).
 */
class LessThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = "<"
    override fun compare(leftValue: Double, rightValue: Double) = leftValue < rightValue
}

/**
 * Less-than-or-equal expression (`left <= right`).
 */
class LessThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = "<="
    override fun compare(leftValue: Double, rightValue: Double) = leftValue <= rightValue
}

/**
 * Greater-than expression (`left > right`).
 */
class GreaterThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = ">"
    override fun compare(leftValue: Double, rightValue: Double) = leftValue > rightValue
}

/**
 * Greater-than-or-equal expression (`left >= right`).
 */
class GreaterThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = ">="
    override fun compare(leftValue: Double, rightValue: Double) = leftValue >= rightValue
}
