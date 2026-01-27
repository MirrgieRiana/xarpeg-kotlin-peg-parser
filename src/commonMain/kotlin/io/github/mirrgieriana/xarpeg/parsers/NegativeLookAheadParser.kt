package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

class NegativeLookAheadParser(val parser: Parser<*>) : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        val result = context.parseOrNull(parser, start)
        if (result != null) return null
        return ParseResult(Tuple0, start, start)
    }
}

val Parser<*>.negativeLookAhead: Parser<Tuple0> get() = NegativeLookAheadParser(this)

val Parser<*>.not: Parser<Tuple0> get() = this.negativeLookAhead
operator fun Parser<*>.not(): Parser<Tuple0> = this.not
