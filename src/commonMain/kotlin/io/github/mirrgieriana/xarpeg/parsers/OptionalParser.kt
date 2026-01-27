package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple1

/**
 * Parser that makes another parser optional.
 *
 * Always succeeds, producing `null` if the wrapped parser fails to match.
 *
 * @param T The type of value produced by the wrapped parser.
 * @param parser The parser to make optional.
 */
class OptionalParser<out T : Any>(val parser: Parser<T>) : Parser<Tuple1<T?>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple1<T?>> {
        val result = context.parseOrNull(parser, start)
        return if (result != null) {
            ParseResult(Tuple1(result.value), result.start, result.end)
        } else {
            ParseResult(Tuple1(null), start, start)
        }
    }
}

/**
 * Creates a parser that matches this parser zero or one time.
 *
 * The resulting parser always succeeds, producing `Tuple1(value)` on match or `Tuple1(null)` on no match.
 */
val <T : Any> Parser<T>.optional get() = OptionalParser(this)
