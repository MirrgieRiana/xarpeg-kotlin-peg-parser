package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1

class RegexParser(val regex: Regex) : Parser<MatchResult> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<MatchResult>? {
        @OptIn(ExperimentalStdlibApi::class)
        val matchResult = regex.matchAt(context.src, start) ?: return null
        return ParseResult(matchResult, start, matchResult.range.last + 1)
    }

    override val name by lazy { regex.toString() }
}

fun Regex.toParser(): Parser<MatchResult> = RegexParser(this)
val Regex.token: Parser<MatchResult> get() = this.toParser()
operator fun Regex.unaryPlus(): Parser<MatchResult> = this.toParser()

val Regex.capture: Parser<Tuple1<MatchResult>> get() = this.toParser().capture

val Regex.ignore: Parser<Tuple0> get() = this.toParser().ignore
operator fun Regex.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

val Regex.not: Parser<Tuple0> get() = this.toParser().not
operator fun Regex.not(): Parser<Tuple0> = this.toParser().not
