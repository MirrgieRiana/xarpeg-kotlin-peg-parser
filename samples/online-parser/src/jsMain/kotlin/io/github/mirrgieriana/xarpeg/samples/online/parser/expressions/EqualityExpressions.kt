package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

/**
 * Base class for equality operators (==, !=).
 * Subclasses implement [compute] to handle supported type combinations.
 */
abstract class EqualityExpression(
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
        val opCtx = ctx.pushFrame("$operatorSymbol operator", position)
        return compute(opCtx, leftVal, rightVal)
            ?: throw EvaluationException(
                "Operator $operatorSymbol is not defined for ${leftVal.typeName} and ${rightVal.typeName}",
                opCtx,
                opCtx.sourceCode,
            )
    }
}

/**
 * Equality expression (`left == right`).
 */
class EqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityExpression(left, right, position) {
    override val operatorSymbol = "=="
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        val result = left.isEqualTo(right) ?: return null
        return BooleanValue(result)
    }
}

/**
 * Inequality expression (`left != right`).
 */
class NotEqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityExpression(left, right, position) {
    override val operatorSymbol = "!="
    override fun compute(ctx: EvaluationContext, left: Value, right: Value): Value? {
        val result = left.isEqualTo(right) ?: return null
        return BooleanValue(!result)
    }
}
