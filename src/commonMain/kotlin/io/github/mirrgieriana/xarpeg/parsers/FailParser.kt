package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * A parser that always fails.
 *
 * This can be useful as a placeholder or default case in conditional parsing logic.
 */
object FailParser : Parser<Nothing> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Nothing>? {
        return null
    }
}

/**
 * Returns a parser that always fails.
 */
val fail get() = FailParser
