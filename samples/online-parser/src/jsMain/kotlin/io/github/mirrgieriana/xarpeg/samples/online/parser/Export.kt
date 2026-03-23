@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.formatMessage
import io.github.mirrgieriana.xarpeg.parseAll
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.FunctionCallExpression
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
data class ExpressionResult(
    val success: Boolean,
    val output: String
)

@JsExport
fun evaluateExpression(input: String): ExpressionResult {
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
