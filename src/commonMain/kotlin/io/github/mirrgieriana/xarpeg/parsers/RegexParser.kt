package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1

/**
 * Parser that matches a regular expression pattern.
 *
 * The regex must match at the current position (anchored match).
 *
 * @param regex The regular expression pattern to match.
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
 * Converts this regular expression to a parser.
 */
fun Regex.toParser(): Parser<MatchResult> = RegexParser(this)

/**
 * Returns a parser that matches this regular expression.
 */
val Regex.token: Parser<MatchResult> get() = this.toParser()

/**
 * Converts this regular expression to a parser using the unary `+` operator.
 *
 * Example: `+Regex("[0-9]+")` creates a parser that matches one or more digits.
 */
operator fun Regex.unaryPlus(): Parser<MatchResult> = this.toParser()

/**
 * Returns a parser that matches this regex and captures the result as a [Tuple1].
 */
val Regex.capture: Parser<Tuple1<MatchResult>> get() = this.toParser().capture

/**
 * Returns a parser that matches this regex but discards the result.
 */
val Regex.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * Converts this regex to a parser that matches it but discards the result.
 */
operator fun Regex.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * Returns a negative lookahead parser for this regex.
 */
val Regex.not: Parser<Tuple0> get() = this.toParser().not

/**
 * Creates a negative lookahead parser for this regex using the `!` operator.
 */
operator fun Regex.not(): Parser<Tuple0> = this.toParser().not
