package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1

private class RegexParser(val regex: Regex) : Parser<MatchResult> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<MatchResult>? {
        @OptIn(ExperimentalStdlibApi::class)
        val matchResult = regex.matchAt(context.src, start) ?: return null
        return ParseResult(matchResult, start, matchResult.range.last + 1)
    }

    override val name by lazy { regex.toString() }
}


/** この正規表現にマッチする部分を解析するパーサーを返します。 */
fun Regex.toParser(): Parser<MatchResult> = RegexParser(this)

/**
 * この正規表現をトークンに変換します。
 * Xarpegはトークン化を行わないため、このプロパティは実質的に [Regex.toParser] と同じです。
 */
val Regex.token: Parser<MatchResult> get() = this.toParser()

/** この正規表現にマッチする部分を解析するパーサーを返します。 */
operator fun Regex.unaryPlus(): Parser<MatchResult> = this.toParser()


/** この正規表現を [Tuple1] として得るパーサーを返します。 */
val Regex.capture: Parser<Tuple1<MatchResult>> get() = this.toParser().capture


/** この正規表現を無視するパーサーを返します。 */
val Regex.ignore: Parser<Tuple0> get() = this.toParser().ignore

/** この正規表現を無視するパーサーを返します。 */
operator fun Regex.unaryMinus(): Parser<Tuple0> = this.toParser().ignore


/** 解析位置直後の文字列がこの正規表現にマッチしないことを確認するパーサーを返します。 */
val Regex.not: Parser<Tuple0> get() = this.toParser().not

/** 解析位置直後の文字列がこの正規表現にマッチしないことを確認するパーサーを返します。 */
operator fun Regex.not(): Parser<Tuple0> = this.toParser().not
