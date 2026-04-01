package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * Base type for all runtime values produced by expression evaluation.
 */
sealed class Value {
    /**
     * The name of this value's type, used in error messages.
     */
    abstract val typeName: String

    /**
     * Tests equality with [other]. Returns `true`/`false` if the types are compatible,
     * or `null` if the types cannot be compared.
     */
    abstract fun isEqualTo(other: Value): Boolean?
}

/**
 * A numeric value. Integers are displayed without a decimal point.
 */
data class NumberValue(val value: Double) : Value() {
    override val typeName = "Number"
    override fun isEqualTo(other: Value) = if (other is NumberValue) value == other.value else null
    override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

/**
 * A boolean value.
 */
data class BooleanValue(val value: Boolean) : Value() {
    override val typeName = "Boolean"
    override fun isEqualTo(other: Value) = if (other is BooleanValue) value == other.value else null
    override fun toString() = value.toString()
}

/**
 * A string value.
 */
data class StringValue(val value: String) : Value() {
    override val typeName = "String"
    override fun isEqualTo(other: Value) = if (other is StringValue) value == other.value else null
    override fun toString() = value
}

/**
 * A callable lambda value. Holds a reference to the [closureScope] where the lambda was defined,
 * enabling lexical scoping when the lambda is called.
 */
data class LambdaValue(
    val params: List<String>,
    val body: Expression,
    val closureScope: VariableTable,
    val definitionPosition: ParseResult<*>? = null,
) : Value() {
    override val typeName = "Lambda"
    override fun isEqualTo(other: Value): Boolean? = null
    override fun toString() = "<lambda(${params.joinToString(", ")})>"
}
