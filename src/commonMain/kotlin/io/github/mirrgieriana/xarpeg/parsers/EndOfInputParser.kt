package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * Parser that succeeds only at the end of the input.
 *
 * Useful for ensuring that the entire input has been consumed.
 */
object EndOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != context.src.length) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * Returns a parser that succeeds only at the end of the input.
 */
val endOfInput = EndOfInputParser
