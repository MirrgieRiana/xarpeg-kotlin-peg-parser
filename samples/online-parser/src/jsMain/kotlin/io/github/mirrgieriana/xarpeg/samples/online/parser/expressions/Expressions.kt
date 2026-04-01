package io.github.mirrgieriana.xarpeg.samples.online.parser.expressions

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.samples.online.parser.BooleanValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.CallFrame
import io.github.mirrgieriana.xarpeg.samples.online.parser.EvaluationException
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.Expression.EvaluationContext
import io.github.mirrgieriana.xarpeg.samples.online.parser.LambdaValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.NumberValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Statement
import io.github.mirrgieriana.xarpeg.samples.online.parser.StringValue
import io.github.mirrgieriana.xarpeg.samples.online.parser.Value

// -- Atoms --

/**
 * A numeric literal expression.
 */
class NumberLiteralExpression(
    private val value: NumberValue,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value = value
}

/**
 * A heredoc literal expression. Evaluates to a [StringValue] containing the heredoc content.
 */
class HeredocExpression(
    private val content: String,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value = StringValue(content)
}

/**
 * A variable reference expression. Throws if the variable is undefined.
 */
class VariableReferenceExpression(
    private val name: String,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        ctx.variableTable.get(name) ?: throw EvaluationException("Undefined variable: $name", ctx.callStack + CallFrame(name, position), ctx.session.sourceCode)
}

/**
 * A lambda expression. Produces a [LambdaValue] when evaluated.
 */
class LambdaExpression(
    private val params: List<String>,
    private val body: Expression,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext) =
        LambdaValue(params, body, ctx.variableTable, definitionPosition = position)
}

/**
 * A function call expression. Evaluates arguments, binds them to parameters, and evaluates the body.
 */
class FunctionCallExpression(
    private val name: String,
    private val args: List<Expression>,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val func = ctx.variableTable.get(name)
            ?: throw EvaluationException("Undefined function: $name", ctx.callStack + CallFrame(name, position), ctx.session.sourceCode)

        if (func !is LambdaValue) {
            throw EvaluationException("$name is not a function", ctx.callStack + CallFrame(name, position), ctx.session.sourceCode)
        }

        if (args.size != func.params.size) {
            throw EvaluationException(
                "Function $name expects ${func.params.size} arguments, but got ${args.size}",
                ctx.callStack + CallFrame(name, position),
                ctx.session.sourceCode,
            )
        }

        val bodyContext = EvaluationContext(
            ctx.session,
            ctx.callStack + CallFrame(name, position),
            func.closureScope.createChild(),
        )
        bodyContext.session.incrementCallCount(bodyContext.callStack)

        func.params.zip(args).forEach { (param, argExpr) ->
            bodyContext.variableTable.define(param, argExpr.evaluate(ctx))
        }

        return func.body.evaluate(bodyContext)
    }
}

// -- Control flow --

/**
 * A ternary conditional expression (`condition ? trueExpr : falseExpr`).
 */
class TernaryExpression(
    private val condition: Expression,
    private val trueExpression: Expression,
    private val falseExpression: Expression,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val condVal = condition.evaluate(ctx)
        if (condVal !is BooleanValue) {
            throw EvaluationException(
                "Condition of ternary operator must be Boolean, got ${condVal.typeName}",
                ctx.callStack + CallFrame("ternary operator", position),
                ctx.session.sourceCode,
            )
        }
        return if (condVal.value) trueExpression.evaluate(ctx) else falseExpression.evaluate(ctx)
    }
}

// -- Top-level --

/**
 * A variable reassignment expression. Updates an existing variable in the scope chain.
 * Throws if the variable has not been declared.
 */
class AssignmentExpression(
    private val name: String,
    private val valueExpression: Expression,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val value = valueExpression.evaluate(ctx)
        if (!ctx.variableTable.set(name, value)) {
            throw EvaluationException("Undefined variable: $name", ctx.callStack + CallFrame(name, position), ctx.session.sourceCode)
        }
        return value
    }
}

/**
 * A sequence of statements. Executes each statement in order and returns the last evaluated value.
 */
class StatementsExpression(
    private val statements: List<Statement>,
    override val position: ParseResult<*>,
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val execCtx = Statement.ExecutionContext(ctx.session, ctx.callStack, ctx.variableTable)
        statements.forEach { it.execute(execCtx) }
        return execCtx.lastValue ?: NumberValue(0.0)
    }
}
