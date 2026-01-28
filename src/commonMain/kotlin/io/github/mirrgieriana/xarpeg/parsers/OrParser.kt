package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

private class OrParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        parsers.forEach { parser ->
            val result = context.parseOrNull(parser, start)
            if (result != null) return result
        }
        return null
    }
}

/**
 * 与えられたパーサーを順番に試すパーサーを作成します。
 */
fun <T : Any> or(vararg parsers: Parser<T>): Parser<T> = OrParser(parsers.toList())

/**
 * `+`演算子を使用して順序選択パーサーを作成します。
 *
 * 例: `parser1 + parser2`は最初にparser1を試し、失敗したらparser2を試します。
 */
operator fun <T : Any> Parser<T>.plus(other: Parser<T>): Parser<T> {
    val leftParsers = if (this is OrParser<T>) this.parsers else listOf(this)
    val rightParsers = if (other is OrParser<T>) other.parsers else listOf(other)
    return OrParser(leftParsers + rightParsers)
}
