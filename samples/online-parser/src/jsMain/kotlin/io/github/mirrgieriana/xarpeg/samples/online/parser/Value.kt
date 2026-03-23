package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

sealed class Value

data class NumberValue(val value: Double) : Value() {
    override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

data class BooleanValue(val value: Boolean) : Value() {
    override fun toString() = value.toString()
}

data class LambdaValue(
    val params: List<String>,
    val body: Expression,
    val capturedVars: MutableMap<String, Value>,
    val name: String? = null,
    val definitionPosition: ParseResult<*>? = null
) : Value() {
    override fun toString() = "<lambda(${params.joinToString(", ")})>"
}
