package mirrg.xarpite.parser.parsers

import mirrg.xarpite.parser.ParseContext
import mirrg.xarpite.parser.ParseResult
import mirrg.xarpite.parser.Parser

class AndParser<T : Any>(val parser: Parser<T>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        val result = context.parseOrNull(parser, start)
        if (result == null) return null
        return ParseResult(result.value, start, start)
    }
}

fun <T : Any> Parser<T>.and() = AndParser(this)
