package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

class FixedParser<T : Any>(val value: T) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return ParseResult(value, start, start)
    }
}

fun <T : Any> fixed(value: T) = FixedParser(value)

val empty get() = fixed(Tuple0)
