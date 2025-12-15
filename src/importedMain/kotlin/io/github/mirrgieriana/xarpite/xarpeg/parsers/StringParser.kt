package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
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

fun String.toParser() = if (isNative) StringParser(this) else StringParser.cache.getOrPut(this) { StringParser(this) }
operator fun String.unaryPlus() = this.toParser()
operator fun String.unaryMinus() = -+this
operator fun String.not() = !+this
