package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

/**
 * Base class for binary arithmetic operators (+, -, *, /).
 * Subclasses implement [compute] to handle supported type combinations.
 */
abstract class ArithmeticExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>,
) : Expression {
    abstract val operatorSymbol: String

    /**
     * Computes the result for the given operand values.
     * Returns `null` if the type combination is not supported by this operator.
     */
    abstract fun compute(ctx: EvaluationContext, left: Value, right: Value): Value?

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)
        return compute(ctx, leftVal, rightVal)
            ?: throw EvaluationException(
                "Operator $operatorSymbol is not defined for ${leftVal.typeName} and ${rightVal.typeName}",
                ctx.callStack + CallFrame("$operatorSymbol operator", position),
                ctx.session.sourceCode,
            )
    }
}

/**
 * Addition expression (`left + right`).
 */
class AddExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticExpression(left, right, position) {
    override val operatorSymbol = "+"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return NumberValue(left.value + right.value)
        return null
    }
}

/**
 * Subtraction expression (`left - right`).
 */
class SubtractExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticExpression(left, right, position) {
    override val operatorSymbol = "-"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return NumberValue(left.value - right.value)
        return null
    }
}

/**
 * Multiplication expression (`left * right`).
 */
class MultiplyExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticExpression(left, right, position) {
    override val operatorSymbol = "*"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return NumberValue(left.value * right.value)
        return null
    }
}

/**
 * Division expression (`left / right`). Throws on division by zero.
 */
class DivideExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ArithmeticExpression(left, right, position) {
    override val operatorSymbol = "/"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) {
            if (right.value == 0.0) throw EvaluationException(
                "Division by zero",
                ctx.callStack + CallFrame("/ operator", position),
                ctx.session.sourceCode,
            )
            return NumberValue(left.value / right.value)
        }
        return null
    }
}
