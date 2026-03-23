package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
import io.github.mirrgieriana.xarpeg.samples.online.parser.requireNumber

/**
 * Base class for binary arithmetic operators (+, -, *, /).
 */
abstract class ArithmeticOperatorExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>,
) : Expression {
    abstract val operatorSymbol: String
    abstract fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double): Double

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)
        val leftNum = leftVal.requireNumber(ctx, operatorSymbol, "Left")
        val rightNum = rightVal.requireNumber(ctx, operatorSymbol, "Right")
        return NumberValue(compute(ctx, leftNum, rightNum))
    }
}

/**
 * Addition expression (`left + right`).
 */
class AddExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "+"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue + rightValue
}

/**
 * Subtraction expression (`left - right`).
 */
class SubtractExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "-"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue - rightValue
}

/**
 * Multiplication expression (`left * right`).
 */
class MultiplyExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "*"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue * rightValue
}

/**
 * Division expression (`left / right`). Throws on division by zero.
 */
class DivideExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "/"

    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double): Double {
        require(rightValue != 0.0) {
            val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("division", position))
            throw EvaluationException("Division by zero", newCtx, ctx.sourceCode)
        }
        return leftValue / rightValue
    }
}
