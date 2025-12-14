package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

/**
 * Functional interface representing an expression that can be evaluated.
 * Replaces the lambda type (EvaluationContext) -> Value for better code organization.
 */
fun interface Expression {
    fun evaluate(ctx: EvaluationContext): Value
}

/**
 * Functional interface representing a binary operator.
 * Takes the left operand and returns a function that evaluates with context.
 */
fun interface BinaryOperator {
    fun apply(left: Value, ctx: EvaluationContext): Value
}
