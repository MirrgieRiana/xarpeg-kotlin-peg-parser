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
 * パーサーへの遅延参照を作成します。
 *
 * これは再帰文法に不可欠です。例:
 * ```
 * val expr: Parser<Int> = object {
 *     val number = +Regex("[0-9]+") map { it.value.toInt() }
 *     val parens = -'(' * ref { expr } * -')'  // 前方参照
 *     val expr = number + parens
 * }.expr
 * ```
 */
fun <T : Any> ref(getter: () -> Parser<T>): Parser<T> = ReferenceParser(getter)
