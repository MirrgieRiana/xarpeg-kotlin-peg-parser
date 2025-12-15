package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
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

fun Char.toParser() = if (isNative) CharParser(this) else CharParser.cache.getOrPut(this) { CharParser(this) }
operator fun Char.unaryPlus() = this.toParser()
operator fun Char.unaryMinus() = -+this
operator fun Char.not() = !+this
