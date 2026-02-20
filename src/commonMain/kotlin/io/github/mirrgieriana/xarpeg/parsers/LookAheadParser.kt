package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.LookAheadHolderParseContext
import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

private class LookAheadParser<T : Any>(val parser: Parser<T>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        val result = if (context is LookAheadHolderParseContext) {
            val oldIsInLookAhead = context.isInLookAhead
            context.isInLookAhead = true
            try {
                context.parseOrNull(parser, start)
            } finally {
                context.isInLookAhead = oldIsInLookAhead
            }
        } else {
            context.parseOrNull(parser, start)
        }
        if (result == null) return null
        return ParseResult(result.value, start, start)
    }
}

val <T : Any> Parser<T>.lookAhead: Parser<T> get() = LookAheadParser(this)
