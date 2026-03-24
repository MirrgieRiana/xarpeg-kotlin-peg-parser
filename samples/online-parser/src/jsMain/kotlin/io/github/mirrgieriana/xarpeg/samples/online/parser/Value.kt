package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * Base type for all runtime values produced by expression evaluation.
 */
sealed class Value {
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
    override fun isEqualTo(other: Value) = if (other is NumberValue) value == other.value else null
    override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

/**
 * A boolean value.
 */
data class BooleanValue(val value: Boolean) : Value() {
    override fun isEqualTo(other: Value) = if (other is BooleanValue) value == other.value else null
    override fun toString() = value.toString()
}

/**
 * A callable lambda value with captured variable scope.
 */
data class LambdaValue(
    val params: List<String>,
    val body: Expression,
    val capturedVars: MutableMap<String, Value>,
    val name: String? = null,
    val definitionPosition: ParseResult<*>? = null,
) : Value() {
    override fun isEqualTo(other: Value): Boolean? = null
    override fun toString() = "<lambda(${params.joinToString(", ")})>"
}

/**
 * Requires this value to be a [NumberValue], throwing an [EvaluationException] otherwise.
 */
fun Value.requireNumber(ctx: EvaluationContext, operatorSymbol: String, side: String): Double {
    if (this !is NumberValue) {
        throw EvaluationException("$side operand of $operatorSymbol must be a number", ctx, ctx.sourceCode)
    }
    return this.value
}

/**
 * Requires this value to be a [BooleanValue], throwing an [EvaluationException] otherwise.
 */
fun Value.requireBoolean(ctx: EvaluationContext, description: String): Boolean {
    if (this !is BooleanValue) {
        throw EvaluationException("$description must be a boolean", ctx, ctx.sourceCode)
    }
    return this.value
}
