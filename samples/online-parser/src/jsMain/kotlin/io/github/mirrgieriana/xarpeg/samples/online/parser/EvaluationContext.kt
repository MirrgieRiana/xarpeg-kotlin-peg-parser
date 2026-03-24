package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * A single frame in the evaluation call stack, used for error reporting.
 */
data class CallFrame(
    val functionName: String,
    val position: ParseResult<*>,
)

/**
 * Scoped variable table with parent chain for lexical scoping.
 */
data class VariableTable(
    val variables: MutableMap<String, Value> = mutableMapOf(),
    val parent: VariableTable? = null,
) {
    /**
     * Looks up a variable by name, searching parent scopes if not found locally.
     */
    fun get(name: String): Value? = variables[name] ?: parent?.get(name)

    /**
     * Sets a variable in the current scope.
     */
    fun set(name: String, value: Value) {
        variables[name] = value
    }

    /**
     * Creates a child scope with this table as its parent.
     */
    fun createChild() = VariableTable(mutableMapOf(), this)
}

/**
 * Runtime context for expression evaluation, holding call stack, source code reference, and variable bindings.
 *
 * The [functionCallCount] is a shared mutable array that survives [copy],
 * allowing total function calls to be tracked across all derived contexts in a single evaluation session.
 */
data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable(),
    private val functionCallCount: IntArray = IntArray(1),
) {
    /**
     * Creates a new context with an additional call frame pushed onto the stack.
     */
    fun pushFrame(functionName: String, callPosition: ParseResult<*>) =
        copy(callStack = callStack + CallFrame(functionName, callPosition))

    /**
     * Creates a new context with a fresh child variable scope.
     */
    fun withNewScope() = copy(variableTable = variableTable.createChild())

    /**
     * Increments the function call count and throws if the limit is exceeded.
     * Prevents infinite recursion from freezing the page.
     */
    fun incrementCallCount() {
        functionCallCount[0]++
        if (functionCallCount[0] >= MAX_FUNCTION_CALLS) {
            throw EvaluationException("Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded", this, sourceCode)
        }
    }

    companion object {
        private const val MAX_FUNCTION_CALLS = 100
    }
}
