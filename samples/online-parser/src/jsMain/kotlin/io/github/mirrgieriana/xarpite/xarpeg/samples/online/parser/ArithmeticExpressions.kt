package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

/**
 * Abstract base class for arithmetic binary operators.
 * Provides common type checking and error handling for numeric operations.
 */
abstract class ArithmeticOperator(
    private val operatorSymbol: String,
    private val snippet: CodeSnippet
) : BinaryOperator {
    
    protected abstract fun compute(leftNum: Double, rightNum: Double): Value
    
    override fun apply(left: Value, ctx: EvaluationContext): Value {
        val right = ctx.rightValue.evaluate(ctx.context)
        
        if (left !is Value.NumberValue) {
            throw EvaluationException("Type error: left operand of $operatorSymbol must be a number", snippet, ctx.context)
        }
        if (right !is Value.NumberValue) {
            throw EvaluationException("Type error: right operand of $operatorSymbol must be a number", snippet, ctx.context)
        }
        
        return compute(left.value, right.value)
    }
}

/**
 * Multiplication operator implementation.
 */
class MultiplyOperator(snippet: CodeSnippet) : ArithmeticOperator("*", snippet) {
    override fun compute(leftNum: Double, rightNum: Double): Value {
        return Value.NumberValue(leftNum * rightNum)
    }
}

/**
 * Division operator implementation with zero-check.
 */
class DivideOperator(private val snippet: CodeSnippet) : ArithmeticOperator("/", snippet) {
    override fun compute(leftNum: Double, rightNum: Double): Value {
        if (rightNum == 0.0) {
            throw EvaluationException("Division by zero", snippet, snippet.context)
        }
        return Value.NumberValue(leftNum / rightNum)
    }
}

/**
 * Addition operator implementation.
 */
class AddOperator(snippet: CodeSnippet) : ArithmeticOperator("+", snippet) {
    override fun compute(leftNum: Double, rightNum: Double): Value {
        return Value.NumberValue(leftNum + rightNum)
    }
}

/**
 * Subtraction operator implementation.
 */
class SubtractOperator(snippet: CodeSnippet) : ArithmeticOperator("-", snippet) {
    override fun compute(leftNum: Double, rightNum: Double): Value {
        return Value.NumberValue(leftNum - rightNum)
    }
}
