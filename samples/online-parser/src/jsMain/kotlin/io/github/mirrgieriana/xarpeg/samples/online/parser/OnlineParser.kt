@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.formatMessage
import io.github.mirrgieriana.xarpeg.parseAll
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.FunctionCallExpression
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

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

data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable()
) {
    fun pushFrame(functionName: String, callPosition: ParseResult<*>) =
        copy(callStack = callStack + CallFrame(functionName, callPosition))

    fun withNewScope() = copy(variableTable = variableTable.createChild())
}

data class CallFrame(val functionName: String, val position: ParseResult<*>)

sealed class Value {
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
}


@JsExport
data class ExpressionResult(
    val success: Boolean,
    val output: String
)

@JsExport
fun parseExpression(input: String): ExpressionResult {
    return try {
        FunctionCallExpression.functionCallCount = 0

        val initialContext = EvaluationContext(sourceCode = input)
        val resultExpr = ExpressionGrammar.programRoot.parseAll(input) { OnlineParserParseContext(it) }.getOrThrow()
        val result = resultExpr.evaluate(initialContext)
        ExpressionResult(success = true, output = result.toString())
    } catch (e: EvaluationException) {
        val errorMessage = if (e.context != null && e.context.callStack.isNotEmpty()) {
            e.formatWithCallStack()
        } else {
            "Error: ${e.message}"
        }
        ExpressionResult(success = false, output = errorMessage)
    } catch (e: ParseException) {
        ExpressionResult(success = false, output = e.formatMessage())
    } catch (e: Exception) {
        ExpressionResult(success = false, output = "Error: ${e.message}")
    }
}
