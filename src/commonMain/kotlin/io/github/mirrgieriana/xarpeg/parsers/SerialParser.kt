package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * パーサーを逐次的にマッチするパーサー。
 *
 * このパーサーが成功するには、すべてのパーサーが順番にマッチする必要があります。
 *
 * @param T 各パーサーが生成する値の型。
 * @param parsers 順番にマッチするパーサーのリスト。
 */
class SerialParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<List<T>> {
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
fun <T : Any> serial(vararg parsers: Parser<T>) = SerialParser(parsers.toList())
