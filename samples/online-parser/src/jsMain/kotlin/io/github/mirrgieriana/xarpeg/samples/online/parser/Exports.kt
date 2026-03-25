@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.formatMessage
import io.github.mirrgieriana.xarpeg.parseAll
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Result of evaluating an expression, containing success status and output string.
 */
@JsExport
data class ExpressionResult(
    val success: Boolean,
    val output: String,
)

/**
 * Parses and evaluates the given [input] string as an expression program.
 * Returns an [ExpressionResult] with the evaluation output or an error message.
 */
@JsExport
fun evaluateExpression(input: String): ExpressionResult {
    return try {
        val session = Session(sourceCode = input)
        val ctx = Expression.EvaluationContext(session)
        val resultExpr = OnlineParserGrammar.root.parseAll(input) { OnlineParserParseContext(it) }.getOrThrow()
        val result = resultExpr.evaluate(ctx)
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
