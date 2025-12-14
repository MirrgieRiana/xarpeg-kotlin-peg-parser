package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

/**
 * Abstract base class for comparison operators (<, <=, >, >=).
 * Provides common type checking and error handling for ordering comparisons.
 */
abstract class ComparisonOperator(
    private val operatorSymbol: String,
    private val snippet: CodeSnippet
) : BinaryOperator {
    
    protected abstract fun compare(leftNum: Double, rightNum: Double): Boolean
    
    override fun apply(left: Value, ctx: EvaluationContext): Value {
        val right = ctx.rightValue.evaluate(ctx.context)
        
        if (left !is Value.NumberValue) {
            throw EvaluationException("Type error: left operand of $operatorSymbol must be a number", snippet, ctx.context)
        }
        if (right !is Value.NumberValue) {
            throw EvaluationException("Type error: right operand of $operatorSymbol must be a number", snippet, ctx.context)
        }
        
        return Value.BooleanValue(compare(left.value, right.value))
    }
}

/**
 * Less-than operator implementation.
 */
class LessThanOperator(snippet: CodeSnippet) : ComparisonOperator("<", snippet) {
    override fun compare(leftNum: Double, rightNum: Double): Boolean = leftNum < rightNum
}

/**
 * Less-than-or-equal operator implementation.
 */
class LessThanOrEqualOperator(snippet: CodeSnippet) : ComparisonOperator("<=", snippet) {
    override fun compare(leftNum: Double, rightNum: Double): Boolean = leftNum <= rightNum
}

/**
 * Greater-than operator implementation.
 */
class GreaterThanOperator(snippet: CodeSnippet) : ComparisonOperator(">", snippet) {
    override fun compare(leftNum: Double, rightNum: Double): Boolean = leftNum > rightNum
}

/**
 * Greater-than-or-equal operator implementation.
 */
class GreaterThanOrEqualOperator(snippet: CodeSnippet) : ComparisonOperator(">=", snippet) {
    override fun compare(leftNum: Double, rightNum: Double): Boolean = leftNum >= rightNum
}

/**
 * Equality operator implementation.
 */
class EqualityOperator(private val snippet: CodeSnippet) : BinaryOperator {
    override fun apply(left: Value, ctx: EvaluationContext): Value {
        val right = ctx.rightValue.evaluate(ctx.context)
        
        val isEqual = when {
            left is Value.NumberValue && right is Value.NumberValue -> left.value == right.value
            left is Value.BooleanValue && right is Value.BooleanValue -> left.value == right.value
            else -> throw EvaluationException("Type error: equality comparison requires matching types", snippet, ctx.context)
        }
        
        return Value.BooleanValue(isEqual)
    }
}

/**
 * Inequality operator implementation.
 */
class InequalityOperator(private val snippet: CodeSnippet) : BinaryOperator {
    override fun apply(left: Value, ctx: EvaluationContext): Value {
        val right = ctx.rightValue.evaluate(ctx.context)
        
        val isEqual = when {
            left is Value.NumberValue && right is Value.NumberValue -> left.value == right.value
            left is Value.BooleanValue && right is Value.BooleanValue -> left.value == right.value
            else -> throw EvaluationException("Type error: inequality comparison requires matching types", snippet, ctx.context)
        }
        
        return Value.BooleanValue(!isEqual)
    }
}
