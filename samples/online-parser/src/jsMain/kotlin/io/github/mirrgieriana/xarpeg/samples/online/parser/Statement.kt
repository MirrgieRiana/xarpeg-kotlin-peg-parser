package io.github.mirrgieriana.xarpeg.samples.online.parser

/**
 * A unit of execution that mutates an [ExecutionContext].
 * Unlike [Expression], a statement does not return a value directly.
 */
interface Statement {
    /**
     * Executes this statement, potentially updating the context's variable scope and last value.
     */
    fun execute(ctx: ExecutionContext)

    /**
     * Mutable execution state for a block of statements (program or function body).
     * Holds the immutable session and call stack, plus the mutable variable scope and last value.
     */
    class ExecutionContext(
        val session: Session,
        val callStack: List<CallFrame>,
        var variableTable: VariableTable,
        var lastValue: Value? = null,
    )
}
