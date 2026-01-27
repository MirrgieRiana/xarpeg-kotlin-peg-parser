package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Wraps a parser with a human-readable name.
 *
 * The name appears in error messages to help users understand what was expected at a parse failure.
 *
 * @param T The type of value produced by the wrapped parser.
 * @param parser The parser to wrap.
 * @param name The name to assign to this parser.
 */
class NamedParser<T : Any>(val parser: Parser<T>, override val name: String) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

/**
 * Assigns a name to this parser for better error messages.
 *
 * Example: `+Regex("[0-9]+") named "number"` will show "number" in error messages.
 */
infix fun <T : Any> Parser<T>.named(name: String) = NamedParser(this, name)
