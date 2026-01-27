package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple1

private class OptionalParser<out T : Any>(val parser: Parser<T>) : Parser<Tuple1<T?>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple1<T?>> {
        val result = context.parseOrNull(parser, start)
        return if (result != null) {
            ParseResult(Tuple1(result.value), result.start, result.end)
        } else {
            ParseResult(Tuple1(null), start, start)
        }
    }
}

/**
 * このパーサーを0回または1回マッチするパーサーを作成します。
 *
 * 結果のパーサーは常に成功し、マッチ時は`Tuple1(value)`、マッチしない場合は`Tuple1(null)`を生成します。
 */
val <T : Any> Parser<T>.optional: Parser<Tuple1<T?>> get() = OptionalParser(this)
