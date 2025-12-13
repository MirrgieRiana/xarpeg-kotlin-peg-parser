@file:OptIn(ExperimentalJsExport::class)

package mirrg.xarpite.samples.online.parser

import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

private object ExpressionGrammar {
    private val whitespace = -Regex("[ \\t\\r\\n]*")

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { it.value.toDouble() }

    private val factor: Parser<Double> by lazy {
        number + (-'(' * whitespace * parser { expression } * whitespace * -')')
    }

    private val product = leftAssociative(factor, whitespace * (+'*' + +'/') * whitespace) { a, op, b ->
        when (op) {
            '*' -> a * b
            '/' -> a / b
            else -> a
        }
    }

    private val expression: Parser<Double> by lazy {
         leftAssociative(product, whitespace * (+'+' + +'-') * whitespace) { a, op, b ->
            when (op) {
                '+' -> a + b
                '-' -> a - b
                else -> a
            }
        } * whitespace
    }

    val root = whitespace * expression * whitespace
}

@JsExport
fun parseExpression(input: String): String =
    try {
        val result = ExpressionGrammar.root.parseAllOrThrow(input)
        result.toString()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
