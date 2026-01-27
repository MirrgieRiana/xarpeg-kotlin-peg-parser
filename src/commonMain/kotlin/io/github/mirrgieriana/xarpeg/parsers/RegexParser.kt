package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1

/**
 * 正規表現パターンをマッチするパーサー。
 *
 * 正規表現は現在位置でマッチする必要があります（アンカーされたマッチ）。
 *
 * @param regex マッチする正規表現パターン。
 */
class RegexParser(val regex: Regex) : Parser<MatchResult> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<MatchResult>? {
        @OptIn(ExperimentalStdlibApi::class)
        val matchResult = regex.matchAt(context.src, start) ?: return null
        return ParseResult(matchResult, start, matchResult.range.last + 1)
    }

    override val name by lazy { regex.toString() }
}

/**
 * この正規表現をパーサーに変換します。
 */
fun Regex.toParser(): Parser<MatchResult> = RegexParser(this)

/**
 * この正規表現をマッチするパーサーを返します。
 */
val Regex.token: Parser<MatchResult> get() = this.toParser()

/**
 * 単項`+`演算子を使用してこの正規表現をパーサーに変換します。
 *
 * 例: `+Regex("[0-9]+")`は1つ以上の数字をマッチするパーサーを作成します。
 */
operator fun Regex.unaryPlus(): Parser<MatchResult> = this.toParser()

/**
 * この正規表現をマッチし、結果を[Tuple1]としてキャプチャするパーサーを返します。
 */
val Regex.capture: Parser<Tuple1<MatchResult>> get() = this.toParser().capture

/**
 * この正規表現をマッチするが、結果を破棄するパーサーを返します。
 */
val Regex.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * この正規表現をマッチするが、結果を破棄するパーサーに変換します。
 */
operator fun Regex.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * この正規表現の否定先読みパーサーを返します。
 */
val Regex.not: Parser<Tuple0> get() = this.toParser().not

/**
 * `!`演算子を使用してこの正規表現の否定先読みパーサーを作成します。
 */
operator fun Regex.not(): Parser<Tuple0> = this.toParser().not
