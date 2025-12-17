@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseException
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.zeroOrMore
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.AddExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.AssignmentExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.DivideExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.EqualsExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.Expression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.FunctionCallExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.GreaterThanExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.GreaterThanOrEqualExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.LambdaExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.LessThanExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.LessThanOrEqualExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.MultiplyExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.NotEqualsExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.NumberLiteralExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.ProgramExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.SubtractExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.TernaryExpression
import io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser.expressions.VariableReferenceExpression
import io.github.mirrgieriana.xarpite.xarpeg.text
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// Variable table with inheritance (parent scope lookup)
data class VariableTable(
    val variables: MutableMap<String, Value> = mutableMapOf(),
    val parent: VariableTable? = null
) {
    fun get(name: String): Value? {
        return variables[name] ?: parent?.get(name)
    }

    fun set(name: String, value: Value) {
        variables[name] = value
    }

    fun createChild(): VariableTable {
        return VariableTable(mutableMapOf(), this)
    }
}

// Evaluation context that holds call stack information and variable scope
data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable()
) {
    fun pushFrame(functionName: String, callPosition: SourcePosition): EvaluationContext {
        return copy(callStack = callStack + CallFrame(functionName, callPosition))
    }

    fun withNewScope(): EvaluationContext {
        return copy(variableTable = variableTable.createChild())
    }
}

// Represents a single call frame in the stack
data class CallFrame(val functionName: String, val position: SourcePosition)

// Represents a position in the source code
data class SourcePosition(val start: Int, val end: Int, val text: String) {
    fun formatLineColumn(source: String): String {
        val beforeStart = source.substring(0, start.coerceAtMost(source.length))
        val line = beforeStart.count { it == '\n' } + 1
        val column = start - (beforeStart.lastIndexOf('\n') + 1) + 1
        return "line $line, column $column"
    }

    fun formatWithContext(source: String): String {
        val beforeStart = source.substring(0, start.coerceAtMost(source.length))
        val line = beforeStart.count { it == '\n' } + 1
        val column = start - (beforeStart.lastIndexOf('\n') + 1) + 1

        // Get the line containing this position
        val lineStart = beforeStart.lastIndexOf('\n') + 1
        val lineEnd = source.indexOf('\n', start).let { if (it == -1) source.length else it }
        val sourceLine = source.substring(lineStart, lineEnd)

        // Calculate positions within the line
        val highlightStart = start - lineStart
        val highlightEnd = (end - lineStart).coerceAtMost(sourceLine.length)

        // Build the formatted output with highlighted range
        val before = sourceLine.substring(0, highlightStart)
        val highlighted = sourceLine.substring(highlightStart, highlightEnd)
        val after = sourceLine.substring(highlightEnd)

        return "line $line, column $column: $before[$highlighted]$after"
    }
}

// Value types that can be stored in variables
sealed class Value {
    data class NumberValue(val value: Double) : Value() {
        override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
    data class BooleanValue(val value: Boolean) : Value() {
        override fun toString() = value.toString()
    }
    data class LambdaValue(
        val params: List<String>,
        val body: Expression,
        val capturedVars: MutableMap<String, Value>,
        val name: String? = null,
        val definitionPosition: SourcePosition? = null
    ) : Value() {
        override fun toString() = "<lambda(${params.joinToString(", ")})>"
    }
}

// Custom exception that includes call stack
class EvaluationException(
    message: String,
    val context: EvaluationContext? = null,
    val sourceCode: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    fun formatWithCallStack(): String {
        val sb = StringBuilder()
        sb.append("Error: $message")

        if (context != null && context.callStack.isNotEmpty()) {
            context.callStack.asReversed().forEach { frame ->
                val location = if (sourceCode != null) {
                    frame.position.formatWithContext(sourceCode)
                } else {
                    "position ${frame.position.start}-${frame.position.end}: ${frame.position.text}"
                }
                sb.append("\n  at $location")
            }
        }

        return sb.toString()
    }
}

private object ExpressionGrammar {
    private val whitespace = -Regex("[ \\t\\r\\n]*")

    // Identifier: alphanumeric and _, but first character cannot be a digit
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value }

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) }

    // Helper function for left-associative binary operator aggregation
    // Takes a term parser and operators that create expressions
    private fun leftAssociativeBinaryOp(
        term: Parser<Expression>,
        operators: Parser<(Expression) -> Expression>
    ): Parser<Expression> {
        return (term * operators.zeroOrMore) map { (first, rest) ->
            var result = first
            rest.forEach { opFunc ->
                result = opFunc(result)
            }
            result
        }
    }

    // Variable reference
    private val variableRef: Parser<Expression> = identifier map { name ->
        VariableReferenceExpression(name)
    }

    // Helper to parse comma-separated list of identifiers
    private val identifierList: Parser<List<String>> = run {
        val restItem = whitespace * -',' * whitespace * identifier
        (identifier * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Lambda parameter list: (param1, param2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty parameter lists: ()
    private val paramList: Parser<List<String>> =
        -'(' * whitespace * (identifierList + (whitespace map { emptyList<String>() })) * whitespace * -')'

    // Lambda expression: (param1, param2, ...) -> body
    private val lambda: Parser<Expression> =
        ((paramList * whitespace * -Regex("->") * whitespace * ref { expression }) mapEx { parseCtx, result ->
            val (params, bodyParser) = result.value
            val lambdaText = result.text(parseCtx)
            val position = SourcePosition(result.start, result.end, lambdaText)
            LambdaExpression(params, bodyParser, position)
        })

    // Helper to parse comma-separated list of expressions
    private val exprList: Parser<List<Expression>> = run {
        val restItem = whitespace * -',' * whitespace * ref { expression }
        (ref { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Argument list for function calls: (arg1, arg2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty argument lists: ()
    private val argList: Parser<List<Expression>> =
        -'(' * whitespace * (exprList + (whitespace map { emptyList<Expression>() })) * whitespace * -')'

    // Function call: identifier(arg1, arg2, ...)
    private val functionCall: Parser<Expression> =
        ((identifier * whitespace * argList) mapEx { parseCtx, result ->
            val (name, args) = result.value
            val callText = result.text(parseCtx)
            val callPosition = SourcePosition(result.start, result.end, callText)
            FunctionCallExpression(name, args, callPosition, parseCtx.src)
        })

    // Primary expression: number, variable reference, function call, lambda, or grouped expression
    private val primary: Parser<Expression> =
        lambda + functionCall + variableRef + (number map { v -> NumberLiteralExpression(v) }) +
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor: Parser<Expression> = primary

    // Multiplication operator parser
    private val multiplyOp = (whitespace * +'*' * whitespace * factor) mapEx { parseCtx, result ->
        // Skip leading whitespace in position tracking by using the operator's position
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '*' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> MultiplyExpression(left, rightExpr, opPosition) }
    }

    // Division operator parser
    private val divideOp = (whitespace * +'/' * whitespace * factor) mapEx { parseCtx, result ->
        // Skip leading whitespace in position tracking by using the operator's position
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '/' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> DivideExpression(left, rightExpr, opPosition) }
    }

    private val product: Parser<Expression> =
        leftAssociativeBinaryOp(factor, multiplyOp + divideOp)

    // Addition operator parser
    private val addOp = (whitespace * +'+' * whitespace * product) mapEx { parseCtx, result ->
        // Skip leading whitespace in position tracking by using the operator's position
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '+' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> AddExpression(left, rightExpr, opPosition) }
    }

    // Subtraction operator parser
    private val subtractOp = (whitespace * +'-' * whitespace * product) mapEx { parseCtx, result ->
        // Skip leading whitespace in position tracking by using the operator's position
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '-' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> SubtractExpression(left, rightExpr, opPosition) }
    }

    private val sum: Parser<Expression> =
        leftAssociativeBinaryOp(product, addOp + subtractOp)

    // Ordering comparison operators: <, <=, >, >=
    private val orderingComparison: Parser<Expression> = run {
        // Less than or equal operator parser (must come before < to match correctly)
        val lessEqualOp = (whitespace * +"<=" * whitespace * sum) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("<=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> LessThanOrEqualExpression(left, rightExpr, opPosition) }
        }

        // Greater than or equal operator parser (must come before > to match correctly)
        val greaterEqualOp = (whitespace * +">=" * whitespace * sum) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf(">=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> GreaterThanOrEqualExpression(left, rightExpr, opPosition) }
        }

        // Less than operator parser
        val lessOp = (whitespace * +'<' * whitespace * sum) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '<' }
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> LessThanExpression(left, rightExpr, opPosition) }
        }

        // Greater than operator parser
        val greaterOp = (whitespace * +'>' * whitespace * sum) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '>' }
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> GreaterThanExpression(left, rightExpr, opPosition) }
        }

        val restItem = lessEqualOp + greaterEqualOp + lessOp + greaterOp

        leftAssociativeBinaryOp(sum, restItem)
    }

    // Equality comparison operators: ==, !=
    private val equalityComparison: Parser<Expression> = run {
        // Equality operator parser
        val equalOp = (whitespace * +"==" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("==")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> EqualsExpression(left, rightExpr, opPosition) }
        }

        // Inequality operator parser
        val notEqualOp = (whitespace * +"!=" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            // Skip leading whitespace in position tracking by using the operator's position
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("!=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> NotEqualsExpression(left, rightExpr, opPosition) }
        }

        val restItem = equalOp + notEqualOp

        leftAssociativeBinaryOp(orderingComparison, restItem)
    }

    // Ternary operator: condition ? trueExpr : falseExpr
    private val ternary: Parser<Expression> = run {
        val ternaryExpr = ref { equalityComparison } * whitespace * -'?' * whitespace *
            ref { equalityComparison } * whitespace * -':' * whitespace *
            ref { equalityComparison }
        ((ternaryExpr mapEx { parseCtx, result ->
            val (cond, trueExpr, falseExpr) = result.value
            val ternaryText = result.text(parseCtx)
            val ternaryPosition = SourcePosition(result.start, result.end, ternaryText)
            TernaryExpression(cond, trueExpr, falseExpr, ternaryPosition)
        }) + equalityComparison)
    }

    // Assignment: variable = expression
    private val assignment: Parser<Expression> = run {
        ((identifier * whitespace * -'=' * whitespace * ref { expression }) map { (name, valueExpr) ->
            AssignmentExpression(name, valueExpr)
        }) + ternary
    }

    // Root expression parser
    val expression: Parser<Expression> = assignment

    // Multi-statement parser: parses multiple expressions separated by newlines
    val program: Parser<Expression> = run {
        val newlineSep = -Regex("[ \\t]*\\r?\\n[ \\t\\r\\n]*")
        ((expression * (newlineSep * expression).zeroOrMore) map { (first, rest) ->
            ProgramExpression(listOf(first) + rest)
        })
    }

    val root = whitespace * expression * whitespace
    val programRoot = whitespace * program * whitespace
}

// Format a ParseException with detailed syntax error information
private fun formatParseException(e: ParseException, input: String): String {
    val sb = StringBuilder()

    // Extract position information
    val position = e.context.errorPosition

    // Calculate line and column numbers in a single pass
    val beforePosition = input.substring(0, position.coerceAtMost(input.length))
    var line = 1
    var lastNewlinePos = -1
    beforePosition.forEachIndexed { i, char ->
        if (char == '\n') {
            line++
            lastNewlinePos = i
        }
    }
    val column = position - lastNewlinePos

    // Build error message
    sb.append("Error: Syntax error at line $line, column $column")

    // Add suggested parsers if available
    if (e.context.suggestedParsers.isNotEmpty()) {
        val candidates = e.context.suggestedParsers
            .mapNotNull { it.name }
            .distinct()
            .take(5)
        if (candidates.isNotEmpty()) {
            sb.append("\nExpected: ${candidates.joinToString(", ")}")
        }
    }

    // Show the line with error indicator
    val lineStart = beforePosition.lastIndexOf('\n') + 1
    val lineEnd = input.indexOf('\n', position).let { if (it == -1) input.length else it }
    val sourceLine = input.substring(lineStart, lineEnd)

    if (sourceLine.isNotEmpty()) {
        sb.append("\n")
        sb.append(sourceLine)
        sb.append("\n")
        // Add caret pointing to the error position
        val caretPosition = position - lineStart
        sb.append(" ".repeat(caretPosition.coerceAtLeast(0)))
        sb.append("^")
    }

    return sb.toString()
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        // Reset function call counter for each evaluation to ensure each call is independent
        FunctionCallExpression.functionCallCount = 0

        // Create initial evaluation context with empty call stack, source code, and fresh variable table
        val initialContext = EvaluationContext(sourceCode = input)

        // Try to parse as a multi-statement program first (handles both single and multiple expressions)
        val resultExpr = ExpressionGrammar.programRoot.parseAllOrThrow(input)
        val result = resultExpr.evaluate(initialContext)
        result.toString()
    } catch (e: EvaluationException) {
        // Use custom formatting if call stack is available
        if (e.context != null && e.context.callStack.isNotEmpty()) {
            e.formatWithCallStack()
        } else {
            "Error: ${e.message}"
        }
    } catch (e: ParseException) {
        // Format parse exceptions with detailed syntax error information
        formatParseException(e, input)
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
