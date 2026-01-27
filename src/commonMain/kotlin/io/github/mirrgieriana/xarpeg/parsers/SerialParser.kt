package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

private class SerialParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<List<T>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<List<T>>? {
        val results = mutableListOf<T>()
        var nextIndex = start
        parsers.forEach { parser ->
            val result = context.parseOrNull(parser, nextIndex) ?: return null
            results += result.value
            nextIndex = result.end
        }
        return ParseResult(results, start, nextIndex)
    }
}

/**
 * 与えられたすべてのパーサーを順番にマッチするパーサーを作成します。
 */
fun <T : Any> serial(vararg parsers: Parser<T>): Parser<List<T>> = SerialParser(parsers.toList())
