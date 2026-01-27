package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 複数の代替パーサーを順番に試すパーサー。
 *
 * 最初にマッチしたパーサーで成功します。これはPEGの順序選択演算子を実装しています。
 *
 * @param T すべての選択肢が生成する値の型。
 * @param parsers 試行する代替パーサーのリスト。
 */
class OrParser<out T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
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
fun <T : Any> or(vararg parsers: Parser<T>) = OrParser(parsers.toList())

/**
 * `+`演算子を使用して順序選択パーサーを作成します。
 *
 * 例: `parser1 + parser2`は最初にparser1を試し、失敗したらparser2を試します。
 */
operator fun <T : Any> Parser<T>.plus(other: Parser<T>) = OrParser(listOf(this, other))

/**
 * 既存の[OrParser]に別の選択肢を追加します。
 */
operator fun <T : Any> OrParser<T>.plus(other: Parser<T>) = OrParser(this.parsers + other)
