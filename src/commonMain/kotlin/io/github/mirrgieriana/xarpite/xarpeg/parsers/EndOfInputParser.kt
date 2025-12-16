package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0

/**
 * A parser that matches only at the end of input.
 * Does not consume any characters.
 *
 * Example:
 * ```
 * val parser = +"hello" * endOfInput
 * parser.parseAllOrThrow("hello")  // succeeds
 * parser.parseAllOrThrow("hello world")  // fails
 * ```
 */
object EndOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != context.src.length) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * Parser that matches at the end of input.
 */
val endOfInput = EndOfInputParser
