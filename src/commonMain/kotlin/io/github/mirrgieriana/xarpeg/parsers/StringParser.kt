package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

/**
 * リテラル文字列をマッチするパーサー。
 *
 * @param string マッチする文字列。
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
 * この文字列をリテラルにマッチするパーサーに変換します。
 */
fun String.toParser(): Parser<String> = if (isNative) StringParser(this) else StringParser.cache.getOrPut(this) { StringParser(this) }

/**
 * この文字列をマッチするパーサーを返します。
 */
val String.token: Parser<String> get() = this.toParser()

/**
 * 単項`+`演算子を使用してこの文字列をパーサーに変換します。
 *
 * 例: `+"hello"`は文字列"hello"をマッチするパーサーを作成します。
 */
operator fun String.unaryPlus(): Parser<String> = this.toParser()

/**
 * この文字列をマッチし、[Tuple1]としてキャプチャするパーサーを返します。
 */
val String.capture: Parser<Tuple1<String>> get() = this.toParser().capture

/**
 * この文字列をマッチするが、結果を破棄するパーサーを返します。
 */
val String.ignore: Parser<Tuple0> get() = this.toParser().ignore

/**
 * この文字列をマッチするが、結果を破棄するパーサーに変換します。
 *
 * 例: `-"("`は'('をマッチするがキャプチャしないパーサーを作成します。
 */
operator fun String.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

/**
 * この文字列の否定先読みパーサーを返します。
 */
val String.not: Parser<Tuple0> get() = this.toParser().not

/**
 * `!`演算子を使用してこの文字列の否定先読みパーサーを作成します。
 */
operator fun String.not(): Parser<Tuple0> = this.toParser().not
