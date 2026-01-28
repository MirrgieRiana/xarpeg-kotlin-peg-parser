package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

private class NamedParser<T : Any>(val parser: Parser<T>, override val name: String) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

/**
 * より良いエラーメッセージのためにこのパーサーに名前を割り当てます。
 *
 * 例: `+Regex("[0-9]+") named "number"`はエラーメッセージに"number"を表示します。
 */
infix fun <T : Any> Parser<T>.named(name: String): Parser<T> = NamedParser(this, name)
