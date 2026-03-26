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
     * Defines a new variable in the current scope.
     */
    fun define(name: String, value: Value) {
        variables[name] = value
    }

    /**
     * Sets an existing variable by searching parent scopes. Throws if not found.
     */
    fun set(name: String, value: Value) {
        if (variables.containsKey(name)) {
            variables[name] = value
        } else {
            parent?.set(name, value) ?: throw IllegalStateException("Undefined variable: $name")
        }
    }

    /**
     * Creates a child scope with this table as its parent.
     */
    fun createChild() = VariableTable(mutableMapOf(), this)
}
