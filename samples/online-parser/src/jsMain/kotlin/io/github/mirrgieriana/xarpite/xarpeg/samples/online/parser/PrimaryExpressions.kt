package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

/**
 * Literal value expression (numbers).
 */
class LiteralExpression(private val value: Value) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value = value
}

/**
 * Variable reference expression.
 */
class VariableExpression(
    private val name: String,
    private val snippet: CodeSnippet
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        return ctx[name] ?: throw EvaluationException("Undefined variable: $name", snippet, ctx)
    }
}

/**
 * Variable assignment expression.
 */
class AssignmentExpression(
    private val name: String,
    private val valueExpr: Expression
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val value = valueExpr.evaluate(ctx)
        ctx[name] = value
        return value
    }
}

/**
 * Function call expression.
 */
class FunctionCallExpression(
    private val functionExpr: Expression,
    private val args: List<Expression>,
    private val snippet: CodeSnippet
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val function = functionExpr.evaluate(ctx)
        
        if (function !is Value.LambdaValue) {
            throw EvaluationException("Type error: not a function", snippet, ctx)
        }
        
        if (args.size != function.params.size) {
            throw EvaluationException(
                "Argument count mismatch: expected ${function.params.size}, got ${args.size}",
                snippet,
                ctx
            )
        }
        
        val newContext = EvaluationContext(ctx)
        for ((param, argExpr) in function.params.zip(args)) {
            newContext[param] = argExpr.evaluate(ctx)
        }
        
        return try {
            function.body.evaluate(newContext)
        } catch (e: EvaluationException) {
            throw e.addStackFrame(snippet)
        }
    }
}

/**
 * Ternary conditional expression.
 */
class TernaryExpression(
    private val condition: Expression,
    private val trueExpr: Expression,
    private val falseExpr: Expression,
    private val snippet: CodeSnippet
) : Expression {
    override fun evaluate(ctx: EvaluationContext): Value {
        val condValue = condition.evaluate(ctx)
        
        if (condValue !is Value.BooleanValue) {
            throw EvaluationException("Type error: ternary condition must be a boolean", snippet, ctx)
        }
        
        return if (condValue.value) {
            trueExpr.evaluate(ctx)
        } else {
            falseExpr.evaluate(ctx)
        }
    }
}
