package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

private object EndOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != context.src.length) return null
        return ParseResult(Tuple0, start, start)
    }

    override val name: String = "EOF"
}

/** 解析位置がソースの末尾である場合にマッチするパーサーを返します。 */
val endOfInput: Parser<Tuple0> = EndOfInputParser
