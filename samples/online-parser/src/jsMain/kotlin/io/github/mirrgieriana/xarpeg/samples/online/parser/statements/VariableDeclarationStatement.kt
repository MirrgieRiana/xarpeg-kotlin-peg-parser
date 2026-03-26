package io.github.mirrgieriana.xarpeg.samples.online.parser.statements

import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement

/**
 * A variable declaration statement (`var name = value`).
 * Binds a new variable in the current scope. Resets [Statement.ExecutionContext.lastValue].
 */
class VariableDeclarationStatement(
    private val name: String,
    private val value: Expression,
) : Statement {
    override fun execute(ctx: Statement.ExecutionContext) {
        val evaluated = value.evaluate(Expression.EvaluationContext(ctx.session, ctx.callStack, ctx.variableTable))
        val newScope = ctx.variableTable.createChild()
        newScope.define(name, evaluated)
        ctx.variableTable = newScope
        ctx.lastValue = null
    }
}
