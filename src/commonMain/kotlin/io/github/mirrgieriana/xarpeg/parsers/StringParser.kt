package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

private class StringParser(val string: String) : Parser<String> {
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


/** この文字列と完全に一致する部分を解析するパーサーを返します。 */
fun String.toParser(): Parser<String> = if (isNative) StringParser(this) else StringParser.cache.getOrPut(this) { StringParser(this) }

/**
 * この文字列をトークンに変換します。
 * Xarpegはトークン化を行わないため、このプロパティは実質的に [String.toParser] と同じです。
 */
val String.token: Parser<String> get() = this.toParser()

/** この文字列と完全に一致する部分を解析するパーサーを返します。 */
operator fun String.unaryPlus(): Parser<String> = this.toParser()


/** この文字列を [Tuple1] として得るパーサーを返します。 */
val String.capture: Parser<Tuple1<String>> get() = this.toParser().capture


/** この文字列を無視するパーサーを返します。 */
val String.ignore: Parser<Tuple0> get() = this.toParser().ignore

/** この文字列を無視するパーサーを返します。 */
operator fun String.unaryMinus(): Parser<Tuple0> = this.toParser().ignore


/** 解析位置直後の文字列がこの文字列にマッチしないことを確認するパーサーを返します。 */
val String.not: Parser<Tuple0> get() = this.toParser().not

/** 解析位置直後の文字列がこの文字列にマッチしないことを確認するパーサーを返します。 */
operator fun String.not(): Parser<Tuple0> = this.toParser().not
