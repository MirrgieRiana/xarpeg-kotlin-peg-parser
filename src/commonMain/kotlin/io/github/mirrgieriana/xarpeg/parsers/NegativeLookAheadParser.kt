package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * Parser that succeeds if the wrapped parser fails, without consuming input.
 *
 * This is a negative lookahead assertion. It checks that the pattern cannot be matched
 * at the current position without advancing the parse position.
 *
 * @param parser The parser that must not match.
 */
class NegativeLookAheadParser(val parser: Parser<*>) : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        val result = context.parseOrNull(parser, start)
        if (result != null) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * Creates a negative lookahead parser.
 *
 * The parser succeeds if this parser would fail, but doesn't consume any input.
 */
val Parser<*>.negativeLookAhead: Parser<Tuple0> get() = NegativeLookAheadParser(this)

/**
 * Creates a negative lookahead parser (alias for [negativeLookAhead]).
 */
val Parser<*>.not: Parser<Tuple0> get() = this.negativeLookAhead

/**
 * Creates a negative lookahead parser using the `!` operator.
 */
operator fun Parser<*>.not(): Parser<Tuple0> = this.not
