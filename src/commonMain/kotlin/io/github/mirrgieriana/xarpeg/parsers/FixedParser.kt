package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * A parser that always succeeds with a fixed value without consuming input.
 *
 * @param T The type of the fixed value.
 * @param value The value to always produce.
 */
class FixedParser<T : Any>(val value: T) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return ParseResult(value, start, start)
    }
}

/**
 * Creates a parser that always succeeds with the given value without consuming input.
 */
fun <T : Any> fixed(value: T) = FixedParser(value)

/**
 * A parser that always succeeds without consuming input, producing [Tuple0].
 *
 * Useful for optional branches or as a default case.
 */
val empty get() = fixed(Tuple0)
