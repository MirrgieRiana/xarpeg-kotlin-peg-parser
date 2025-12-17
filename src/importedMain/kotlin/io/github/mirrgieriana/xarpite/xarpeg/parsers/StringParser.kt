package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1
import io.github.mirrgieriana.xarpite.xarpeg.impl.escapeDoubleQuote
import io.github.mirrgieriana.xarpite.xarpeg.isNative

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

fun String.toParser(): Parser<String> = if (isNative) StringParser(this) else StringParser.cache.getOrPut(this) { StringParser(this) }
val String.token: Parser<String> get() = this.toParser()
operator fun String.unaryPlus(): Parser<String> = this.toParser()

val String.capture: Parser<Tuple1<String>> get() = this.toParser().capture

val String.ignore: Parser<Tuple0> get() = this.toParser().ignore
operator fun String.unaryMinus(): Parser<Tuple0> = this.toParser().ignore

val String.not: Parser<Tuple0> get() = this.toParser().not
operator fun String.not(): Parser<Tuple0> = this.toParser().not
