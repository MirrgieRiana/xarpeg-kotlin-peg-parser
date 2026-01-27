@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.SourcePosition
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
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

        if (func !is Value.LambdaValue) {
            throw EvaluationException("$name is not a function", ctx, ctx.sourceCode)
        }

        if (args.size != func.params.size) {
            throw EvaluationException(
                "Function $name expects ${func.params.size} arguments, but got ${args.size}",
                ctx,
                sourceCode
            )
        }

        functionCallCount++
        if (functionCallCount >= MAX_FUNCTION_CALLS) {
            throw EvaluationException(
                "Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded",
                ctx,
                sourceCode
            )
        }

        val newContext = ctx.pushFrame(name, position).withNewScope()

        func.params.zip(args).forEach { (param, argExpr) ->
            newContext.variableTable.set(param, argExpr.evaluate(ctx))
        }

        return func.body.evaluate(newContext)
    }

    companion object {
        var functionCallCount = 0
        private const val MAX_FUNCTION_CALLS = 100
    }
}
