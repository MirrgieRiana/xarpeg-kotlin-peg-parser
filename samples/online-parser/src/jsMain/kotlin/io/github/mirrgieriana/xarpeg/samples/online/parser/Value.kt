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

fun Value.requireNumber(ctx: EvaluationContext, operatorSymbol: String, side: String): Double {
    require(this is NumberValue) {
        throw EvaluationException("$side operand of $operatorSymbol must be a number", ctx, ctx.sourceCode)
    }
    return value
}

fun Value.requireBoolean(ctx: EvaluationContext, description: String): Boolean {
    require(this is BooleanValue) {
        throw EvaluationException("$description must be a boolean", ctx, ctx.sourceCode)
    }
    return value
}
