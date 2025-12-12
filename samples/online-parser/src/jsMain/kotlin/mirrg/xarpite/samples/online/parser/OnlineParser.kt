@file:OptIn(ExperimentalJsExport::class)

package mirrg.xarpite.samples.online.parser

import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

private object ExpressionGrammar {
    private val whitespace = -Regex("\\s*")

    private fun <T : Any> token(parser: Parser<T>): Parser<T> =
        whitespace * parser * whitespace

    private val number = token(+Regex("[0-9]+(?:\\.[0-9]+)?")) map { it.value.toDouble() }

    private val grouped: Parser<Double> by lazy {
        token(-'(' * parser { expression } * -')')
    }

    private val factor: Parser<Double> = number + grouped

    private val product = leftAssociative(factor, token(+'*' + +'/')) { a, op, b ->
        when (op) {
            '*' -> a * b
            '/' -> a / b
            else -> a
        }
    }

    val expression: Parser<Double> = leftAssociative(product, token(+'+' + +'-')) { a, op, b ->
        when (op) {
            '+' -> a + b
            '-' -> a - b
            else -> a
        }
    }
}

@JsExport
fun parseExpression(input: String): String =
    try {
        val result = ExpressionGrammar.expression.parseAllOrThrow(input)
        result.toString()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
