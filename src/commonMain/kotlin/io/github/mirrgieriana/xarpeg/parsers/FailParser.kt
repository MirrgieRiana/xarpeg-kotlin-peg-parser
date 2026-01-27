package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

object FailParser : Parser<Nothing> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Nothing>? {
        return null
    }
}

val fail get() = FailParser
