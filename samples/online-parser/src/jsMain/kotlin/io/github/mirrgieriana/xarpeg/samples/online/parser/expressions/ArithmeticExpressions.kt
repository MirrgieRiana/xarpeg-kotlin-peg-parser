@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.SourcePosition
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
abstract class ArithmeticOperatorExpression(
    protected val left: Expression,
    protected val right: Expression,
    protected val position: SourcePosition
) : Expression {
    abstract val operatorSymbol: String
    abstract fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double): Double

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)
        val leftNum = leftVal.requireNumber(ctx, operatorSymbol, "Left")
        val rightNum = rightVal.requireNumber(ctx, operatorSymbol, "Right")
        return Value.NumberValue(compute(ctx, leftNum, rightNum))
    }
}

@JsExport
class AddExpression(left: Expression, right: Expression, position: SourcePosition) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "+"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue + rightValue
}

@JsExport
class SubtractExpression(left: Expression, right: Expression, position: SourcePosition) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "-"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue - rightValue
}

@JsExport
class MultiplyExpression(left: Expression, right: Expression, position: SourcePosition) :
    ArithmeticOperatorExpression(left, right, position) {
    override val operatorSymbol = "*"
    override fun compute(ctx: EvaluationContext, leftValue: Double, rightValue: Double) = leftValue * rightValue
}

@JsExport
class DivideExpression(left: Expression, right: Expression, position: SourcePosition) :
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
