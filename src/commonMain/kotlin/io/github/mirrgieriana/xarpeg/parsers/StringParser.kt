package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

/**
 * Parser that matches a literal string.
 *
 * @param string The string to match.
 */
class StringParser(val string: String) : Parser<String> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<String>? {
        val nextIndex = start + string.length
        if (nextIndex > context.src.length) return null
        var index = 0
        while (index < string.length) {
            if (context.src[start + index] != string[index]) return null
            index++
        }
        return ParseResult(string, start, nextIndex)
    }

    override val name by lazy { "\"" + string.escapeDoubleQuote() + "\"" }

    companion object {
        val cache = mutableMapOf<String, StringParser>()
    }
}

/**
 * Converts this string to a parser that matches it literally.
 */
fun String.toParser(): Parser<String> = if (isNative) StringParser(this) else StringParser.cache.getOrPut(this) { StringParser(this) }

/**
 * Returns a parser that matches this string.
 */
val String.token: Parser<String> get() = this.toParser()

/**
 * Converts this string to a parser using the unary `+` operator.
 *
 * Example: `+"hello"` creates a parser that matches the string "hello".
 */
operator fun String.unaryPlus(): Parser<String> = this.toParser()

/**
 * Returns a parser that matches this string and captures it as a [Tuple1].
 */
val String.capture: Parser<Tuple1<String>> get() = this.toParser().capture

/**
 * Returns a parser that matches this string but discards the result.
 */
val String.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * Converts this string to a parser that matches it but discards the result.
 *
 * Example: `-"("` creates a parser that matches '(' without capturing it.
 */
operator fun String.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * Returns a negative lookahead parser for this string.
 */
val String.not: Parser<Tuple0> get() = this.toParser().not

/**
 * Creates a negative lookahead parser for this string using the `!` operator.
 */
operator fun String.not(): Parser<Tuple0> = this.toParser().not
