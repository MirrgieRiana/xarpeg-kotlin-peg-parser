package io.github.mirrgieriana.xarpite.xarpeg.samples.java_run

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

private val expression: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { match -> match.value.toInt() }
    val grouped: Parser<Int> = -'(' * ref { sum } * -')' map { value -> value }
    val factor: Parser<Int> = number + grouped
    val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
}.sum

fun main() {
    val input = "2*(3+4)+5"
    val result = expression.parseAllOrThrow(input)
    println("$input = $result")
}
