package io.github.mirrgieriana.xarpeg.samples.online.parser

/**
 * Mutable execution state for a block of statements (program or function body).
 * Statements mutate this frame rather than returning values.
 */
class ExecutionFrame(
    var context: EvaluationContext,
    var lastValue: Value? = null,
)
