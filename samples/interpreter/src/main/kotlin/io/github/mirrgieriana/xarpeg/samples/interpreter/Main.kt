package io.github.mirrgieriana.xarpeg.samples.interpreter

import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.formatMessage
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus

class DivisionByZeroException(
    message: String,
    val position: Int,
    val line: Int,
    val column: Int
) : Exception(message)

data class LazyValue(val position: Int, val compute: () -> Int)

data class OperatorInfo(val position: Int, val op: Char)

private object ArithmeticParser {
    val number: Parser<LazyValue> = +Regex("[0-9]+") mapEx { _, result ->
        val value = result.value.value.toInt()
        LazyValue(result.start) { value }
    } named "number"

    val grouped: Parser<LazyValue> = -'(' * ref { expr } * -')'

    val primary: Parser<LazyValue> = number + grouped

    val product: Parser<LazyValue> = leftAssociative(
        primary,
        (+'*' mapEx { _, r -> OperatorInfo(r.start, '*') }) + (+'/' mapEx { _, r -> OperatorInfo(r.start, '/') })
    ) { a, op, b ->
        when (op.op) {
            '*' -> LazyValue(op.position) { a.compute() * b.compute() }
            '/' -> LazyValue(op.position) {
                val divisor = b.compute()
                if (divisor == 0) {
                    throw DivisionByZeroException(
                        "Division by zero",
                        op.position,
                        0,
                        0
                    )
                }
                a.compute() / divisor
            }
            else -> error("Unknown operator: ${op.op}")
        }
    }

    val sum: Parser<LazyValue> = leftAssociative(product, +'+' + +'-') { a, op, b ->
        when (op) {
            '+' -> LazyValue(a.position) { a.compute() + b.compute() }
            '-' -> LazyValue(a.position) { a.compute() - b.compute() }
            else -> error("Unknown operator: $op")
        }
    }

    val expr: Parser<LazyValue> = sum
}

fun indexToPosition(text: String, index: Int): Pair<Int, Int> {
    val lineStartIndices = mutableListOf(0)
    text.forEachIndexed { i, char ->
        if (char == '\n') lineStartIndices.add(i + 1)
    }

    val lineIndex = lineStartIndices.binarySearch(index).let {
        if (it >= 0) it else -it - 2
    }
    val lineStart = lineStartIndices[lineIndex]
    val line = lineIndex + 1
    val column = index - lineStart + 1

    return Pair(line, column)
}

fun evaluate(expression: String): Int {
    val lazyResult = ArithmeticParser.expr.parseAllOrThrow(expression)

    try {
        return lazyResult.compute()
    } catch (e: DivisionByZeroException) {
        val (line, column) = indexToPosition(expression, e.position)

        throw DivisionByZeroException(
            e.message ?: "Division by zero",
            e.position,
            line,
            column
        )
    }
}

fun main(args: Array<String>) {
    when {
        args.isEmpty() || args[0] != "-e" -> {
            println("Usage: interpreter -e <expression>")
            println("Example: interpreter -e \"2+3*4\"")
        }
        args.size < 2 -> {
            println("Error: No expression provided")
            println("Usage: interpreter -e <expression>")
        }
        else -> {
            try {
                println(evaluate(args[1]))
            } catch (e: ParseException) {
                println(e.formatMessage())
            } catch (e: DivisionByZeroException) {
                println("Error: ${e.message} at line ${e.line}, column ${e.column}")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}
