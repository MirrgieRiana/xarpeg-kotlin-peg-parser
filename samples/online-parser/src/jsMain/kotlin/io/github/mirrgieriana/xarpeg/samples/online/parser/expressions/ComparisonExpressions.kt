package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

/**
 * Base class for ordering comparison operators (<, <=, >, >=).
 * Subclasses implement [compute] to handle supported type combinations.
 */
abstract class ComparisonExpression(
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
 * Less-than expression (`left < right`).
 */
class LessThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = "<"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return BooleanValue(left.value < right.value)
        return null
    }
}

/**
 * Less-than-or-equal expression (`left <= right`).
 */
class LessThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = "<="
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return BooleanValue(left.value <= right.value)
        return null
    }
}

/**
 * Greater-than expression (`left > right`).
 */
class GreaterThanExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = ">"
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return BooleanValue(left.value > right.value)
        return null
    }
}

/**
 * Greater-than-or-equal expression (`left >= right`).
 */
class GreaterThanOrEqualExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    ComparisonExpression(left, right, position) {
    override val operatorSymbol = ">="
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        if (left is NumberValue && right is NumberValue) return BooleanValue(left.value >= right.value)
        return null
    }
}
