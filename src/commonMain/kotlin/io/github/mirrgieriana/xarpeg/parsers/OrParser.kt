package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Parser that tries multiple alternative parsers in order.
 *
 * Succeeds with the first parser that matches. This implements PEG's ordered choice operator.
 *
 * @param T The type of value produced by all alternatives.
 * @param parsers The list of alternative parsers to try.
 */
class OrParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        parsers.forEach { parser ->
            val result = context.parseOrNull(parser, start)
            if (result != null) return result
        }
        return null
    }
}

/**
 * Creates a parser that tries each of the given parsers in order.
 */
fun <T : Any> or(vararg parsers: Parser<T>) = OrParser(parsers.toList())

/**
 * Creates an ordered choice parser using the `+` operator.
 *
 * Example: `parser1 + parser2` tries parser1 first, then parser2 if the first fails.
 */
operator fun <T : Any> Parser<T>.plus(other: Parser<T>) = OrParser(listOf(this, other))

/**
 * Adds another alternative to an existing [OrParser].
 */
operator fun <T : Any> OrParser<T>.plus(other: Parser<T>) = OrParser(this.parsers + other)
