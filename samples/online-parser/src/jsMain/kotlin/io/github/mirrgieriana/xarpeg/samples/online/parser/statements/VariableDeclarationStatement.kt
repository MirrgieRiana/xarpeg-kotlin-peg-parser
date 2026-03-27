package io.github.mirrgieriana.xarpeg.samples.online.parser.statements

import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement

/**
 * A variable declaration statement (`var name = value`).
 * Creates a child scope, evaluates the value within it, and defines the variable there.
 * Replaces the current scope with the child. Resets [Statement.ExecutionContext.lastValue].
 */
class VariableDeclarationStatement(
    private val name: String,
    private val value: Expression,
) : Statement {
    override fun execute(ctx: Statement.ExecutionContext) {
        val newScope = ctx.variableTable.createChild()
        val evaluated = value.evaluate(Expression.EvaluationContext(ctx.session, ctx.callStack, newScope))
        newScope.define(name, evaluated)
        ctx.variableTable = newScope
        ctx.lastValue = null
    }
}
