package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

/**
 * 単一の文字をマッチするパーサー。
 *
 * @param char マッチする文字。
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
 * この文字をマッチするパーサーに変換します。
 */
fun Char.toParser(): Parser<Char> = if (isNative) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }

/**
 * この文字をマッチするパーサーを返します。
 */
val Char.token: Parser<Char> get() = this.toParser()

/**
 * 単項`+`演算子を使用してこの文字をパーサーに変換します。
 *
 * 例: `+'a'`は文字'a'をマッチするパーサーを作成します。
 */
operator fun Char.unaryPlus(): Parser<Char> = this.toParser()

/**
 * この文字をマッチし、[Tuple1]としてキャプチャするパーサーを返します。
 */
val Char.capture: Parser<Tuple1<Char>> get() = this.toParser().capture

/**
 * この文字をマッチするが、結果を破棄するパーサーを返します。
 */
val Char.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * この文字をマッチするが、結果を破棄するパーサーに変換します。
 *
 * 例: `-'a'`は'a'をマッチするがキャプチャしないパーサーを作成します。
 */
operator fun Char.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * この文字の否定先読みパーサーを返します。
 */
val Char.not: Parser<Tuple0> get() = this.toParser().not

/**
 * `!`演算子を使用してこの文字の否定先読みパーサーを作成します。
 */
operator fun Char.not(): Parser<Tuple0> = this.toParser().not
