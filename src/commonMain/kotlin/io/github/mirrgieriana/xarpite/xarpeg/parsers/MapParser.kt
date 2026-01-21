package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser

infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(result.value), result.start, result.end)
}

infix fun <I : Any, O : Any> Parser<I>.mapEx(function: (ParseContext, ParseResult<I>) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(context, result), result.start, result.end)
}

val <T : Any> Parser<T>.result get() = this.mapEx { _, result -> result }
