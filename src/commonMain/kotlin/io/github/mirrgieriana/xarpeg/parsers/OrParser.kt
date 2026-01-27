package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

class OrParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        parsers.forEach { parser ->
            val result = context.parseOrNull(parser, start)
            if (result != null) return result
        }
        return null
    }
}

fun <T : Any> or(vararg parsers: Parser<T>) = OrParser(parsers.toList())
operator fun <T : Any> Parser<T>.plus(other: Parser<T>) = OrParser(listOf(this, other))
operator fun <T : Any> OrParser<T>.plus(other: Parser<T>) = OrParser(this.parsers + other)
