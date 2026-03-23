package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

class TernaryExpression(
    private val condition: Expression,
    private val trueExpression: Expression,
    private val falseExpression: Expression,
    override val position: ParseResult<*>
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val condVal = condition.evaluate(ctx)
        val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("ternary operator", position))
        val condBool = condVal.requireBoolean(newCtx, "Condition in ternary operator")
        return if (condBool) trueExpression.evaluate(ctx) else falseExpression.evaluate(ctx)
    }
}
