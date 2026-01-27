package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * 入力の開始位置（位置0）でのみ成功するパーサー。
 *
 * 入力の開始位置から始まる必要があるパターンをアンカーするのに便利です。
 */
object StartOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != 0) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * 入力の開始位置でのみ成功するパーサーを返します。
 */
val startOfInput = StartOfInputParser
