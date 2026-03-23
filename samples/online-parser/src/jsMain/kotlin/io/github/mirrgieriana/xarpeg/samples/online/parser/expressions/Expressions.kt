package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.LambdaValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value
import io.github.mirrgieriana.xarpeg.samples.online.parser.requireBoolean

class NumberLiteralExpression(private val value: NumberValue, override val position: ParseResult<*>) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value = value
}

class VariableReferenceExpression(private val name: String, override val position: ParseResult<*>) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        ctx.variableTable.get(name) ?: throw EvaluationException("Undefined variable: $name", ctx, ctx.sourceCode)
}

class AssignmentExpression(private val name: String, private val valueExpression: Expression, override val position: ParseResult<*>) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val value = valueExpression.evaluate(ctx)
        ctx.variableTable.set(name, value)
        return value
    }
}

class LambdaExpression(private val params: List<String>, private val body: Expression, override val position: ParseResult<*>) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        LambdaValue(params, body, mutableMapOf(), definitionPosition = position)
}

class ProgramExpression(private val expressions: List<Expression>, override val position: ParseResult<*>) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        expressions.fold(NumberValue(0.0) as Value) { _, expr -> expr.evaluate(ctx) }
}

class TernaryExpression(
    private val condition: Expression,
    private val trueExpression: Expression,
    private val falseExpression: Expression,
    override val position: ParseResult<*>
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val condVal = condition.evaluate(ctx)
        val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("ternary operator", position))
        val condBool = condVal.requireBoolean(newCtx, "Condition in ternary operator")
        return if (condBool) trueExpression.evaluate(ctx) else falseExpression.evaluate(ctx)
    }
}

class FunctionCallExpression(
    private val name: String,
    private val args: List<Expression>,
    override val position: ParseResult<*>
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val func = ctx.variableTable.get(name)
            ?: throw EvaluationException("Undefined function: $name", ctx, ctx.sourceCode)

        if (func !is LambdaValue) {
            throw EvaluationException("$name is not a function", ctx, ctx.sourceCode)
        }

        if (args.size != func.params.size) {
            throw EvaluationException(
                "Function $name expects ${func.params.size} arguments, but got ${args.size}",
                ctx,
                ctx.sourceCode
            )
        }

        functionCallCount++
        if (functionCallCount >= MAX_FUNCTION_CALLS) {
            throw EvaluationException(
                "Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded",
                ctx,
                ctx.sourceCode
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
