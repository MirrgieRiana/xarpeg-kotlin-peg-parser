package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0

/**
 * A parser that matches only at the start of input (position 0).
 * Does not consume any characters.
 *
 * Example:
 * ```
 * val parser = startOfInput * +"hello"
 * parser.parseAllOrThrow("hello")  // succeeds
 * ```
 */
object StartOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != 0) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * Parser that matches at the start of input.
 */
val startOfInput = StartOfInputParser
