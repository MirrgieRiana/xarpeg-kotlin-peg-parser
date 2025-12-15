package io.github.mirrgieriana.xarpite.xarpeg.samples.interpreter

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.named
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus

/**
 * A simple arithmetic interpreter that evaluates expressions with +, -, *, / and parentheses.
 * Only handles integer arithmetic.
 * Reports division by zero with line and column position information.
 */

/**
 * Exception thrown when division by zero is detected during evaluation.
 */
class DivisionByZeroException(
    message: String,
    val position: Int,
    val line: Int,
    val column: Int
) : Exception(message)

/**
 * Represents a lazy evaluation of an arithmetic expression.
 * The position tracks where the operation occurs in the source text.
 */
data class LazyValue(val position: Int, val compute: () -> Int)

/**
 * Represents an operator with its position in the source text.
 */
data class OperatorInfo(val position: Int, val op: Char)

private object ArithmeticParser {
    // Parse a number and wrap it in a lazy value
    val number: Parser<LazyValue> = +Regex("[0-9]+") mapEx { _, result ->
        val value = result.value.value.toInt()
        LazyValue(result.start) { value }
    } named "number"
    
    // Parse a grouped expression with parentheses
    val grouped: Parser<LazyValue> = -'(' * ref { expr } * -')'
    
    // Primary expression: number or grouped
    val primary: Parser<LazyValue> = number + grouped
    
    // Multiplication and division (higher precedence)
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
                        0, // Will be filled in by caller
                        0  // Will be filled in by caller
                    )
                }
                a.compute() / divisor
            }
            else -> error("Unknown operator: ${op.op}")
        }
    }
    
    // Addition and subtraction (lower precedence)
    val sum: Parser<LazyValue> = leftAssociative(product, +'+' + +'-') { a, op, b ->
        when (op) {
            '+' -> LazyValue(a.position) { a.compute() + b.compute() }
            '-' -> LazyValue(a.position) { a.compute() - b.compute() }
            else -> error("Unknown operator: $op")
        }
    }
    
    // Root expression
    val expr: Parser<LazyValue> = sum
}

/**
 * Convert a character index to line and column position.
 */
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

/**
 * Evaluate an arithmetic expression.
 * @param expression The expression to evaluate
 * @return The result of the evaluation
 * @throws DivisionByZeroException if division by zero is encountered
 */
fun evaluate(expression: String): Int {
    val lazyResult = ArithmeticParser.expr.parseAllOrThrow(expression)
    
    try {
        return lazyResult.compute()
    } catch (e: DivisionByZeroException) {
        // Convert position to line and column
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
    if (args.isEmpty() || args[0] != "-e") {
        println("Usage: interpreter -e <expression>")
        println("Example: interpreter -e \"2+3*4\"")
        return
    }
    
    if (args.size < 2) {
        println("Error: No expression provided")
        println("Usage: interpreter -e <expression>")
        return
    }
    
    val expression = args[1]
    
    try {
        val result = evaluate(expression)
        println(result)
    } catch (e: DivisionByZeroException) {
        println("Error: ${e.message} at line ${e.line}, column ${e.column}")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
