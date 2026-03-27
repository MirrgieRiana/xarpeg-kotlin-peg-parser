package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * A single frame in the evaluation call stack, used for error reporting.
 */
data class CallFrame(
    val functionName: String,
    val position: ParseResult<*>,
)
