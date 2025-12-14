package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser

class RegexParser(val regex: Regex) : Parser<MatchResult> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<MatchResult>? {
        @OptIn(ExperimentalStdlibApi::class)
        val matchResult = regex.matchAt(context.src, start) ?: return null
        return ParseResult(matchResult, start, matchResult.range.last + 1)
    }
}

fun Regex.toParser() = RegexParser(this)
operator fun Regex.unaryPlus() = this.toParser()
operator fun Regex.unaryMinus() = -+this
operator fun Regex.not() = !+this
