package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

private class ReferenceParser<out T : Any>(val parserGetter: () -> Parser<T>) : Parser<T> {
    private val parser by lazy { parserGetter() }
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

/**
 * 与えられたパーサーを遅延して参照するパーサーを生成します。
 * [getter] はこのパーサーが最初に実際に使用されるときに1度だけ呼び出されます。
 * このパーサーは再帰的なパーサーを構築する際に利用できます。
 * 具体的には、パーサーの定義位置よりも後ろで定義されるパーサーを参照する際に、このパーサーを利用します。
 */
fun <T : Any> ref(getter: () -> Parser<T>): Parser<T> = ReferenceParser(getter)
