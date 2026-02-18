package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

private object StartOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != 0) return null
        return ParseResult(Tuple0, start, start)
    }
}

/** 解析位置がソースの先頭である場合にマッチするパーサーを返します。 */
val startOfInput: Parser<Tuple0> = StartOfInputParser
