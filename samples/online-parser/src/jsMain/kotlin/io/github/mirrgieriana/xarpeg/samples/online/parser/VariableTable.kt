package io.github.mirrgieriana.xarpeg.samples.online.parser

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
