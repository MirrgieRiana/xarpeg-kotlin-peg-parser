package io.github.mirrgieriana.xarpeg.samples.online.parser

/**
 * A sequence of statements. Executes each statement in order and returns the last evaluated value.
 */
class Program(
    val statements: List<Statement>,
) {
    /**
     * Executes all statements in the given [ctx] and returns the last evaluated expression value.
     */
    fun execute(ctx: EvaluationContext): Value {
        val frame = ExecutionFrame(ctx)
        statements.forEach { it.execute(frame) }
        return frame.lastValue ?: NumberValue(0.0)
    }
}
