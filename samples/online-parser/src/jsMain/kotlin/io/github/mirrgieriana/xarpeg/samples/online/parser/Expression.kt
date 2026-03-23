package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * AST node representing a parsed expression that can be evaluated to produce a [Value].
 */
interface Expression {
    /**
     * Source position of this expression in the input.
     */
    val position: ParseResult<*>

    /**
     * Evaluates this expression within the given [ctx] and returns the resulting [Value].
     */
    fun evaluate(ctx: EvaluationContext): Value
}
