package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

/**
 * Base class for equality operators (==, !=).
 */
abstract class EqualityExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>,
) : Expression {
    abstract val operatorSymbol: String

    /**
     * Compares two values using [Value.isEqualTo] and applies operator-specific logic
     * (identity for `==`, negation for `!=`). Returns `null` if the types are incompatible.
     */
    protected abstract fun compareValues(left: Value, right: Value): Boolean?

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)

        val result = compareValues(leftVal, rightVal)
        if (result == null) {
            val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("$operatorSymbol operator", position))
            throw EvaluationException("Operands of $operatorSymbol are not comparable", newCtx, ctx.sourceCode)
        }

        return BooleanValue(result)
    }
}

/**
 * Equality expression (`left == right`).
 */
class EqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityExpression(left, right, position) {
    override val operatorSymbol = "=="
    override fun compareValues(left: Value, right: Value) = left.isEqualTo(right)
}

/**
 * Inequality expression (`left != right`).
 */
class NotEqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityExpression(left, right, position) {
    override val operatorSymbol = "!="
    override fun compareValues(left: Value, right: Value) = left.isEqualTo(right)?.let { !it }
}
