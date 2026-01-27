package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * 入力の終了位置でのみ成功するパーサー。
 *
 * 入力全体が消費されたことを確認するのに便利です。
 */
object EndOfInputParser : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        if (start != context.src.length) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * 入力の終了位置でのみ成功するパーサーを返します。
 */
val endOfInput = EndOfInputParser
