package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

abstract class EqualityOperatorExpression(
    protected val left: Expression,
    protected val right: Expression,
    override val position: ParseResult<*>
) : Expression {
    abstract val operatorSymbol: String
    abstract fun compareValues(result: Boolean): Boolean

    override fun evaluate(ctx: EvaluationContext): Value {
        val leftVal = left.evaluate(ctx)
        val rightVal = right.evaluate(ctx)

        val compareResult = when {
            leftVal is NumberValue && rightVal is NumberValue -> leftVal.value == rightVal.value
            leftVal is BooleanValue && rightVal is BooleanValue -> leftVal.value == rightVal.value
            else -> {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("$operatorSymbol operator", position))
                throw EvaluationException("Operands of $operatorSymbol must be both numbers or both booleans", newCtx, ctx.sourceCode)
            }
        }

        return BooleanValue(compareValues(compareResult))
    }
}

class EqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityOperatorExpression(left, right, position) {
    override val operatorSymbol = "=="
    override fun compareValues(result: Boolean) = result
}

class NotEqualsExpression(left: Expression, right: Expression, position: ParseResult<*>) :
    EqualityOperatorExpression(left, right, position) {
    override val operatorSymbol = "!="
    override fun compareValues(result: Boolean) = !result
}
