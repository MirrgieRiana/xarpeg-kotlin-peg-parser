package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * ラップされたパーサーが失敗すれば成功し、入力を消費しないパーサー。
 *
 * これは否定先読みアサーションです。パース位置を進めることなく、現在位置でパターンが
 * マッチできないことをチェックします。
 *
 * @param parser マッチしてはいけないパーサー。
 */
class NegativeLookAheadParser(val parser: Parser<*>) : Parser<Tuple0> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Tuple0>? {
        val result = context.parseOrNull(parser, start)
        if (result != null) return null
        return ParseResult(Tuple0, start, start)
    }
}

/**
 * 否定先読みパーサーを作成します。
 *
 * このパーサーが失敗する場合にパーサーは成功しますが、入力を消費しません。
 */
val Parser<*>.negativeLookAhead: Parser<Tuple0> get() = NegativeLookAheadParser(this)

/**
 * 否定先読みパーサーを作成します（[negativeLookAhead]のエイリアス）。
 */
val Parser<*>.not: Parser<Tuple0> get() = this.negativeLookAhead

/**
 * `!`演算子を使用して否定先読みパーサーを作成します。
 */
operator fun Parser<*>.not(): Parser<Tuple0> = this.not
