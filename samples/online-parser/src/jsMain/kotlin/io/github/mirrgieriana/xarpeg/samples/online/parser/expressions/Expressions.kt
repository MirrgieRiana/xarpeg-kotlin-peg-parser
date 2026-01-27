@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.SourcePosition
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
class NumberLiteralExpression(private val value: Value.NumberValue) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value = value
}

@JsExport
class VariableReferenceExpression(private val name: String) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        ctx.variableTable.get(name) ?: throw EvaluationException("Undefined variable: $name", ctx, ctx.sourceCode)
}

@JsExport
class AssignmentExpression(private val name: String, private val valueExpression: Expression) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val value = valueExpression.evaluate(ctx)
        ctx.variableTable.set(name, value)
        return value
    }
}

@JsExport
class LambdaExpression(private val params: List<String>, private val body: Expression, private val position: SourcePosition) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        Value.LambdaValue(params, body, mutableMapOf(), definitionPosition = position)
}

@JsExport
class ProgramExpression(private val expressions: List<Expression>) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        expressions.fold(Value.NumberValue(0.0) as Value) { _, expr -> expr.evaluate(ctx) }
}
