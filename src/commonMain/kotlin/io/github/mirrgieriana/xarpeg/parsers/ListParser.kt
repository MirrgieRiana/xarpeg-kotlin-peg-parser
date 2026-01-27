package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 繰り返しパターンをマッチするパーサー。
 *
 * @param T 各マッチが生成する値の型。
 * @param parser 繰り返すパーサー。
 * @param min 必要な最小マッチ数（含む）。
 * @param max 許可される最大マッチ数（含む）。
 */
class ListParser<out T : Any>(val parser: Parser<T>, val min: Int, val max: Int) : Parser<List<T>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<List<T>>? {
        val results = mutableListOf<T>()
        var nextIndex = start
        while (true) {
            val result = context.parseOrNull(parser, nextIndex) ?: break
            results += result.value
            nextIndex = result.end
            if (results.size >= max) break
        }
        if (results.size < min) return null
        return ParseResult(results, start, nextIndex)
    }
}

/**
 * このパーサーを繰り返しマッチするパーサーを作成します。
 *
 * @param min 必要な最小マッチ数。デフォルトは0。
 * @param max 許可される最大マッチ数。デフォルトは[Int.MAX_VALUE]。
 */
fun <T : Any> Parser<T>.list(min: Int = 0, max: Int = Int.MAX_VALUE) = ListParser(this, min, max)

/**
 * このパーサーを0回以上マッチするパーサーを作成します（クリーネスター）。
 */
val <T : Any> Parser<T>.zeroOrMore get() = this.list()

/**
 * このパーサーを1回以上マッチするパーサーを作成します（クリーネプラス）。
 */
val <T : Any> Parser<T>.oneOrMore get() = this.list(min = 1)
