package mirrg.xarpite.samples.javarun

import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

private val expression: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { match -> match.value.toInt() }
    val grouped: Parser<Int> by lazy { -'(' * parser { sum } * -')' }
    val factor: Parser<Int> = number + grouped
    val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
}.sum

fun main() {
    val input = "2*(3+4)+5"
    val result = expression.parseAllOrThrow(input)
    println("$input = $result")
}
