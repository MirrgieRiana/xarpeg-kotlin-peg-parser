package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser

infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O) = object : Parser<O> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<O>? {
        val result = context.parseOrNull(this@map, start) ?: return null
        return ParseResult(function(result.value), result.start, result.end)
    }
    override val name: String? get() = this@map.name
}

infix fun <I : Any, O : Any> Parser<I>.mapEx(function: (ParseContext, ParseResult<I>) -> O) = object : Parser<O> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<O>? {
        val result = context.parseOrNull(this@mapEx, start) ?: return null
        return ParseResult(function(context, result), result.start, result.end)
    }
    override val name: String? get() = this@mapEx.name
}
