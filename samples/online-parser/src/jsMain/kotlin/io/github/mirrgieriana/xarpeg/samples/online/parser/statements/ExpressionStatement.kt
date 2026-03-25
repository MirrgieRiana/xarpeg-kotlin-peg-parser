package io.github.mirrgieriana.xarpeg.samples.online.parser.statements

import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement

/**
 * A statement that evaluates an expression and stores its result as the frame's last value.
 */
class ExpressionStatement(
    private val expression: Expression,
) : Statement {
    override fun execute(ctx: Statement.ExecutionContext) {
        ctx.lastValue = expression.evaluate(Expression.EvaluationContext(ctx.session, ctx.callStack, ctx.variableTable))
    }
}
