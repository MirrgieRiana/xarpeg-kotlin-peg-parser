package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.Tuple1
import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.isNative

private class CharParser(val char: Char) : Parser<Char> {
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


/** この文字をパースするパーサーを返します。 */
fun Char.toParser(): Parser<Char> = if (isNative) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }

/**
 * この文字をトークンに変換します。
 * Xarpegはトークン化を行わないため、このプロパティは実質的に [Char.toParser] と同じです。
 */
val Char.token: Parser<Char> get() = this.toParser()

/** この文字をパースするパーサーを返します。 */
operator fun Char.unaryPlus(): Parser<Char> = this.toParser()


/** この文字を [Tuple1] として得るパーサーを返します。 */
val Char.capture: Parser<Tuple1<Char>> get() = this.toParser().capture


/** この文字を無視するパーサーを返します。 */
val Char.ignore: Parser<Tuple0> get() = this.toParser().ignore

/** この文字を無視するパーサーを返します。 */
operator fun Char.unaryMinus(): Parser<Tuple0> = this.toParser().ignore


/** 解析位置直後の文字列がこの文字にマッチしないことを確認するパーサーを返します。 */
val Char.not: Parser<Tuple0> get() = this.toParser().not

/** 解析位置直後の文字列がこの文字にマッチしないことを確認するパーサーを返します。 */
operator fun Char.not(): Parser<Tuple0> = this.toParser().not
