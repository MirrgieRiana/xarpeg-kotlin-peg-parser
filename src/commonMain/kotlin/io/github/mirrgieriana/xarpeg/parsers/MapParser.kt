package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O?) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    val mapped = function(result.value) ?: return@Parser null
    ParseResult(mapped, result.start, result.end)
}

infix fun <I : Any, O : Any> Parser<I>.mapEx(function: (ParseContext, ParseResult<I>) -> O?) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    val mapped = function(context, result) ?: return@Parser null
    ParseResult(mapped, result.start, result.end)
}

val <T : Any> Parser<T>.result get() = this.mapEx { _, result -> result }
