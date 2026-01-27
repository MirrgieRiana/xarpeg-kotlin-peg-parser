package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

/**
 * Parser that matches a single character.
 *
 * @param char The character to match.
 */
class CharParser(val char: Char) : Parser<Char> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Char>? {
        if (start >= context.src.length) return null
        if (context.src[start] != char) return null
        return ParseResult(char, start, start + 1)
    }

    override val name by lazy { "\"" + "$char".escapeDoubleQuote() + "\"" }

    companion object {
        val cache = mutableMapOf<Char, CharParser>()
    }
}

/**
 * Converts this character to a parser that matches it.
 */
fun Char.toParser(): Parser<Char> = if (isNative) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }

/**
 * Returns a parser that matches this character.
 */
val Char.token: Parser<Char> get() = this.toParser()

/**
 * Converts this character to a parser using the unary `+` operator.
 *
 * Example: `+'a'` creates a parser that matches the character 'a'.
 */
operator fun Char.unaryPlus(): Parser<Char> = this.toParser()

/**
 * Returns a parser that matches this character and captures it as a [Tuple1].
 */
val Char.capture: Parser<Tuple1<Char>> get() = this.toParser().capture

/**
 * Returns a parser that matches this character but discards the result.
 */
val Char.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * Converts this character to a parser that matches it but discards the result.
 *
 * Example: `-'a'` creates a parser that matches 'a' without capturing it.
 */
operator fun Char.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * Returns a negative lookahead parser for this character.
 */
val Char.not: Parser<Tuple0> get() = this.toParser().not

/**
 * Creates a negative lookahead parser for this character using the `!` operator.
 */
operator fun Char.not(): Parser<Tuple0> = this.toParser().not
