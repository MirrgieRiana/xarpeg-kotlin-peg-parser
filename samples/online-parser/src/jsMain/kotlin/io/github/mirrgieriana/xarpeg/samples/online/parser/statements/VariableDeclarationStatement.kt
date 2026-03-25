package io.github.mirrgieriana.xarpeg.samples.online.parser.statements

import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement

/**
 * A variable declaration statement (`var name = value`).
 * Binds a new variable in the current scope. Does not update [Statement.ExecutionContext.lastValue].
 */
class VariableDeclarationStatement(
    private val name: String,
    private val value: Expression,
) : Statement {
    override fun execute(ctx: Statement.ExecutionContext) {
        val evaluated = value.evaluate(Expression.EvaluationContext(ctx.session, ctx.callStack, ctx.variableTable))
        ctx.variableTable.set(name, evaluated)
    }
}
