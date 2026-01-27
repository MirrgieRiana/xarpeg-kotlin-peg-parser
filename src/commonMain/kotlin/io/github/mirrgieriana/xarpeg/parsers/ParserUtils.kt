package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Normalizes line endings in a string.
 *
 * Converts all line endings (`\r\n` and `\r`) to `\n`.
 */
fun String.normalize() = this.replace("\r\n", "\n").replace("\r", "\n")

/**
 * Creates a left-associative binary operator parser.
 *
 * Parses expressions like `a op b op c` as `((a op b) op c)`.
 *
 * @param T The type of operands and result.
 * @param O The type of operator.
 * @param term Parser for operands.
 * @param operator Parser for the operator.
 * @param combinator Function that combines left operand, operator, and right operand.
 * @return A parser that produces the combined result.
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
 * Creates a right-associative binary operator parser.
 *
 * Parses expressions like `a op b op c` as `(a op (b op c))`.
 *
 * @param T The type of operands and result.
 * @param O The type of operator.
 * @param term Parser for operands.
 * @param operator Parser for the operator.
 * @param combinator Function that combines left operand, operator, and right operand.
 * @return A parser that produces the combined result.
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
