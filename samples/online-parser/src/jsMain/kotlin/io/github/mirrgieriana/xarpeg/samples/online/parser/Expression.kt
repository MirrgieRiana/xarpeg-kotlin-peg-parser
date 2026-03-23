package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

interface Expression {
    val position: ParseResult<*>
    fun evaluate(ctx: EvaluationContext): Value
}
