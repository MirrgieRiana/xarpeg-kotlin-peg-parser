package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 別のパーサーを遅延参照するパーサー。
 *
 * パーサーが自分自身や、まだ定義されていない他のパーサーを参照する必要がある
 * 再帰文法に不可欠です。
 *
 * @param T 参照されるパーサーが生成する値の型。
 * @param parserGetter 委譲先のパーサーを返す関数。
 */
class ReferenceParser<out T : Any>(val parserGetter: () -> Parser<T>) : Parser<T> {
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
fun <T : Any> ref(getter: () -> Parser<T>) = ReferenceParser(getter)
