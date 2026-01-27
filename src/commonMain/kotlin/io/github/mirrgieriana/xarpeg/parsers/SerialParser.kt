package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Parser that matches parsers sequentially.
 *
 * All parsers must match in order for this parser to succeed.
 *
 * @param T The type of value produced by each parser.
 * @param parsers The list of parsers to match in sequence.
 */
class SerialParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<List<T>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<List<T>>? {
        val results = mutableListOf<T>()
        var nextIndex = start
        parsers.forEach { parser ->
            val result = context.parseOrNull(parser, nextIndex) ?: return null
            results += result.value
            nextIndex = result.end
        }
        return ParseResult(results, start, nextIndex)
    }
}

/**
 * Creates a parser that matches all given parsers in sequence.
 */
fun <T : Any> serial(vararg parsers: Parser<T>) = SerialParser(parsers.toList())
