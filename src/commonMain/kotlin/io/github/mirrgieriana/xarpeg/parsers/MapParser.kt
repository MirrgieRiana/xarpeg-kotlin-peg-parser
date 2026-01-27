package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Transforms the result of this parser using the given function.
 *
 * @param I The input type produced by this parser.
 * @param O The output type produced by the transformation.
 * @param function The transformation function to apply to successful parse results.
 * @return A parser that produces transformed values.
 */
infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(result.value), result.start, result.end)
}

/**
 * Transforms the result of this parser using a function that also has access to the parse context.
 *
 * This is useful when the transformation needs additional information from the parsing context,
 * such as the matched text or position information.
 *
 * @param I The input type produced by this parser.
 * @param O The output type produced by the transformation.
 * @param function The transformation function that receives the parse context and result.
 * @return A parser that produces transformed values.
 */
infix fun <I : Any, O : Any> Parser<I>.mapEx(function: (ParseContext, ParseResult<I>) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(context, result), result.start, result.end)
}

/**
 * Creates a parser that produces the [ParseResult] itself instead of just the value.
 *
 * Useful when you need access to position information (start/end) in addition to the parsed value.
 */
val <T : Any> Parser<T>.result get() = this.mapEx { _, result -> result }
