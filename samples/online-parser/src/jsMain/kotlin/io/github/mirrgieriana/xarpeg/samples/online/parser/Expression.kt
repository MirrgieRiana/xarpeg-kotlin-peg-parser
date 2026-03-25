package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * AST node representing a parsed expression that can be evaluated to produce a [Value].
 */
interface Expression {
    /**
     * Source position of this expression in the input.
     */
    val position: ParseResult<*>

    /**
     * Evaluates this expression within the given [ctx] and returns the resulting [Value].
     */
    fun evaluate(ctx: EvaluationContext): Value

    /**
     * Argument bundle for [evaluate]. Combines the session, call stack, and variable scope
     * needed to evaluate an expression.
     */
    class EvaluationContext(
        val session: Session,
        val callStack: List<CallFrame> = emptyList(),
        val variableTable: VariableTable = VariableTable(),
    ) {
        /**
         * Creates a new context for a function call, with a call frame and a child scope derived from [closureScope].
         */
        fun pushFrame(functionName: String, callPosition: ParseResult<*>, closureScope: VariableTable) =
            EvaluationContext(session, callStack + CallFrame(functionName, callPosition), closureScope.createChild())

        /**
         * Increments the function call count via the session.
         */
        fun incrementCallCount() {
            session.incrementCallCount(callStack)
        }
    }
}
