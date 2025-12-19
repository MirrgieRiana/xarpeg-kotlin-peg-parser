@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.SourcePosition
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.Value
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
class FunctionCallExpression(
    private val name: String,
    private val args: List<Expression>,
    private val position: SourcePosition,
    private val sourceCode: String
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val func = ctx.variableTable.get(name)
            ?: throw EvaluationException("Undefined function: $name", ctx, ctx.sourceCode)

        when (func) {
            is Value.LambdaValue -> {
                if (args.size != func.params.size) {
                    throw EvaluationException(
                        "Function $name expects ${func.params.size} arguments, but got ${args.size}",
                        ctx,
                        sourceCode
                    )
                }

                // Check function call limit before making the call
                functionCallCount++
                if (functionCallCount >= MAX_FUNCTION_CALLS) {
                    throw EvaluationException(
                        "Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded",
                        ctx,
                        sourceCode
                    )
                }

                // Create a new scope for the function call
                val newContext = ctx.pushFrame(name, position).withNewScope()

                // Evaluate arguments in the caller's context and bind to parameters in the new scope
                func.params.zip(args).forEach { (param, argExpr) ->
                    newContext.variableTable.set(param, argExpr.evaluate(ctx))
                }

                // Execute function body in the new context
                return func.body.evaluate(newContext)
            }
            else -> throw EvaluationException("$name is not a function", ctx, ctx.sourceCode)
        }
    }

    companion object {
        var functionCallCount = 0
        private const val MAX_FUNCTION_CALLS = 100
    }
}
