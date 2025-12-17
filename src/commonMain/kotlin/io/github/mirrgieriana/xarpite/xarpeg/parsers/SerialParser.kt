package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser

class SerialParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<List<T>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<List<T>>? {
        val results = mutableListOf<T>()
        var nextIndex = start
        for (parser in parsers) {
            val result = context.parseOrNull(parser, nextIndex) ?: return null
            results += result.value
            nextIndex = result.end
        }
        return ParseResult(results, start, nextIndex)
    }
}

fun <T : Any> serial(vararg parsers: Parser<T>) = SerialParser(parsers.toList())
