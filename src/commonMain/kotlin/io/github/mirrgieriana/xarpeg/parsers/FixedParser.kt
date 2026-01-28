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

/**
 * 入力を消費せずに与えられた値で常に成功するパーサーを作成します。
 */
fun <T : Any> fixed(value: T): Parser<T> = FixedParser(value)

/**
 * 入力を消費せずに[Tuple0]を生成して常に成功するパーサー。
 *
 * オプショナルな分岐やデフォルトケースとして便利です。
 */
val empty: Parser<Tuple0> get() = fixed(Tuple0)
