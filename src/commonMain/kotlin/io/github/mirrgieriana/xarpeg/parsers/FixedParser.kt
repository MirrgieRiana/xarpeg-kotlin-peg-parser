package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

private class FixedParser<T : Any>(val value: T) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return ParseResult(value, start, start)
    }
}

/** 解析位置を進めることなしに、常に [value] を返すパーサーを返します。 */
fun <T : Any> fixed(value: T): Parser<T> = FixedParser(value)

/** 解析位置を進めることなしに、常に [Tuple0] を返すパーサーを返します。 */
val empty get() = fixed(Tuple0)
