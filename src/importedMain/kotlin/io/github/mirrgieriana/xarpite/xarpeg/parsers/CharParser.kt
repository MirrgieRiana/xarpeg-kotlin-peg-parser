package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1
import io.github.mirrgieriana.xarpite.xarpeg.impl.escapeDoubleQuote
import io.github.mirrgieriana.xarpite.xarpeg.isNative

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

fun Char.toParser(): Parser<Char> = if (isNative) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }
val Char.token: Parser<Char> get() = this.toParser()
operator fun Char.unaryPlus(): Parser<Char> = this.toParser()

val Char.capture: Parser<Tuple1<Char>> get() = this.toParser().capture

val Char.ignore: Parser<Tuple0> get() = this.toParser().ignore
operator fun Char.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

val Char.not: Parser<Tuple0> get() = this.toParser().not
operator fun Char.not(): Parser<Tuple0> = this.toParser().not
