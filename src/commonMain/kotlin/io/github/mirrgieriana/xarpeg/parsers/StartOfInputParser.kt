package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * Parser that succeeds only at the start of the input (position 0).
 *
 * Useful for anchoring patterns that must begin at the start of the input.
 */
object StartOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != 0) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * Returns a parser that succeeds only at the start of the input.
 */
val startOfInput = StartOfInputParser
