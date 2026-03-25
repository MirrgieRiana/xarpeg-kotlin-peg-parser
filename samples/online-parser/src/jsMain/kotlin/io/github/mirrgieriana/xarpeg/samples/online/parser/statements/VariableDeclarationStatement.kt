package io.github.mirrgieriana.xarpeg.samples.online.parser.statements

import io.github.mirrgieriana.xarpeg.samples.online.parser.ExecutionFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement

/**
 * A variable declaration statement (`var name = value`).
 * Binds a new variable in the current scope. Does not update [ExecutionFrame.lastValue].
 */
class VariableDeclarationStatement(
    private val name: String,
    private val value: Expression,
) : Statement {
    override fun execute(frame: ExecutionFrame) {
        val evaluated = value.evaluate(frame.context)
        frame.context.variableTable.set(name, evaluated)
    }
}
