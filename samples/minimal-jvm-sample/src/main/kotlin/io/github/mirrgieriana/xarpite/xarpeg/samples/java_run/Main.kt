package io.github.mirrgieriana.xarpite.xarpeg.samples.java_run

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.named
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus

private val expression: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { match -> match.value.toInt() } named "number"
    val brackets: Parser<Int> = -'(' * ref { root } * -')'
    val factor = (number + brackets) named "factor"
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    val input = "2*(3+4)+5"
    val result = expression.parseAllOrThrow(input)
    println("$input = $result")
}
