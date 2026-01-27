package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 文字列内の改行を正規化します。
 *
 * すべての改行（`\r\n`と`\r`）を`\n`に変換します。
 */
fun String.normalize() = this.replace("\r\n", "\n").replace("\r", "\n")

/**
 * 左結合二項演算子パーサーを作成します。
 *
 * `a op b op c`のような式を`((a op b) op c)`としてパースします。
 *
 * @param T オペランドと結果の型。
 * @param O 演算子の型。
 * @param term オペランドのパーサー。
 * @param operator 演算子のパーサー。
 * @param combinator 左オペランド、演算子、右オペランドを結合する関数。
 * @return 結合された結果を生成するパーサー。
 */
fun <T : Any, O : Any> leftAssociative(term: Parser<T>, operator: Parser<O>, combinator: (T, O, T) -> T) = Parser { context, start ->
    var result = context.parseOrNull(term, start) ?: return@Parser null
    while (true) {
        val operatorResult = context.parseOrNull(operator, result.end) ?: break
        val rightResult = context.parseOrNull(term, operatorResult.end) ?: break
        result = ParseResult(combinator(result.value, operatorResult.value, rightResult.value), result.start, rightResult.end)
    }
    result
}

/**
 * 右結合二項演算子パーサーを作成します。
 *
 * `a op b op c`のような式を`(a op (b op c))`としてパースします。
 *
 * @param T オペランドと結果の型。
 * @param O 演算子の型。
 * @param term オペランドのパーサー。
 * @param operator 演算子のパーサー。
 * @param combinator 左オペランド、演算子、右オペランドを結合する関数。
 * @return 結合された結果を生成するパーサー。
 */
fun <T : Any, O : Any> rightAssociative(term: Parser<T>, operator: Parser<O>, combinator: (T, O, T) -> T) = Parser { context, start ->
    val termResults = mutableListOf<ParseResult<T>>()
    val operatorResults = mutableListOf<ParseResult<O>>()
    val leftResult = context.parseOrNull(term, start) ?: return@Parser null
    termResults += leftResult
    var nextIndex = leftResult.end
    while (true) {
        val operatorResult = context.parseOrNull(operator, nextIndex) ?: break
        val rightResult = context.parseOrNull(term, operatorResult.end) ?: break
        operatorResults += operatorResult
        termResults += rightResult
        nextIndex = rightResult.end
    }
    if (termResults.size == 1) return@Parser termResults.single()
    var result = termResults.last()
    var i = operatorResults.size - 1
    while (i >= 0) {
        result = ParseResult(combinator(termResults[i].value, operatorResults[i].value, result.value), termResults[i].start, result.end)
        i--
    }
    result
}
