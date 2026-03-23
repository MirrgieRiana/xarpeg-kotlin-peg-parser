package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable()
) {
    fun pushFrame(functionName: String, callPosition: ParseResult<*>) =
        copy(callStack = callStack + CallFrame(functionName, callPosition))

    fun withNewScope() = copy(variableTable = variableTable.createChild())
}

data class VariableTable(
    val variables: MutableMap<String, Value> = mutableMapOf(),
    val parent: VariableTable? = null
) {
    fun get(name: String): Value? = variables[name] ?: parent?.get(name)

    fun set(name: String, value: Value) {
        variables[name] = value
    }

    fun createChild() = VariableTable(mutableMapOf(), this)
}

data class CallFrame(val functionName: String, val position: ParseResult<*>)
