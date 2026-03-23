package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue

abstract class ComparisonOperatorExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>
) : Expression {
    abstract val operatorSymbol: String
    abstract fun compare(leftValue: Double, rightValue: Double): Boolean

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)
        val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("$operatorSymbol operator", position))
        val leftNum = leftVal.requireNumber(newCtx, operatorSymbol, "Left")
        val rightNum = rightVal.requireNumber(newCtx, operatorSymbol, "Right")
        return BooleanValue(compare(leftNum, rightNum))
    }
}

class LessThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonOperatorExpression(left, right, position) {
    override val operatorSymbol = "<"
    override fun compare(leftValue: Double, rightValue: Double) = leftValue < rightValue
}

class LessThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonOperatorExpression(left, right, position) {
    override val operatorSymbol = "<="
    override fun compare(leftValue: Double, rightValue: Double) = leftValue <= rightValue
}

class GreaterThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonOperatorExpression(left, right, position) {
    override val operatorSymbol = ">"
    override fun compare(leftValue: Double, rightValue: Double) = leftValue > rightValue
}

class GreaterThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonOperatorExpression(left, right, position) {
    override val operatorSymbol = ">="
    override fun compare(leftValue: Double, rightValue: Double) = leftValue >= rightValue
}
