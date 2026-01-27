package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Parser that succeeds if the wrapped parser matches, but doesn't consume input.
 *
 * This is a positive lookahead assertion. It checks that the pattern can be matched
 * at the current position without advancing the parse position.
 *
 * @param T The type of value produced by the wrapped parser.
 * @param parser The parser to check.
 */
class LookAheadParser<T : Any>(val parser: Parser<T>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        val result = context.parseOrNull(parser, start)
        if (result == null) return null
        return ParseResult(result.value, start, start)
    }
}

/**
 * Creates a positive lookahead parser.
 *
 * The parser succeeds if this parser would match, but doesn't consume any input.
 */
val <T : Any> Parser<T>.lookAhead get() = LookAheadParser(this)
