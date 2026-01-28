@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.AddExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.AssignmentExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.DivideExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.EqualsExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.Expression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.FunctionCallExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.GreaterThanExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.GreaterThanOrEqualExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LambdaExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LessThanExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LessThanOrEqualExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.MultiplyExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.NotEqualsExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.NumberLiteralExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.ProgramExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.SubtractExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.TernaryExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.VariableReferenceExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.indent.IndentParseContext
import io.github.mirrgieriana.xarpeg.text
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

data class VariableTable(
    val variables: MutableMap<String, Value> = mutableMapOf(),
    val parent: VariableTable? = null
) {
    fun get(name: String): Value? = variables[name] ?: parent?.get(name)

    fun set(name: String, value: Value) {
        variables[name] = value
    }

    fun createChild() = VariableTable(mutableMapOf(), this)
}

data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable()
) {
    fun pushFrame(functionName: String, callPosition: SourcePosition) =
        copy(callStack = callStack + CallFrame(functionName, callPosition))

    fun withNewScope() = copy(variableTable = variableTable.createChild())
}

data class CallFrame(val functionName: String, val position: SourcePosition)

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

        val lineStart = beforeStart.lastIndexOf('\n') + 1
        val lineEnd = source.indexOf('\n', start).let { if (it == -1) source.length else it }
        val sourceLine = source.substring(lineStart, lineEnd)

        val highlightStart = start - lineStart
        val highlightEnd = (end - lineStart).coerceAtMost(sourceLine.length)

        val before = sourceLine.substring(0, highlightStart)
        val highlighted = sourceLine.substring(highlightStart, highlightEnd)
        val after = sourceLine.substring(highlightEnd)

        return "line $line, column $column: $before[$highlighted]$after"
    }
}

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

    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value } named "identifier"

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) } named "number"

    private fun leftAssociativeBinaryOp(
        term: Parser<Expression>,
        operators: Parser<(Expression) -> Expression>
    ) = (term * operators.zeroOrMore) map { (first, rest) ->
        rest.fold(first) { acc, opFunc -> opFunc(acc) }
    }

    private val variableRef: Parser<Expression> = identifier map { name ->
        VariableReferenceExpression(name)
    }

    private val identifierList: Parser<List<String>> = run {
        val restItem = whitespace * -',' * whitespace * identifier
        (identifier * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    private val paramList: Parser<List<String>> =
        -'(' * whitespace * (identifierList + (whitespace map { emptyList<String>() })) * whitespace * -')'

    private val lambda: Parser<Expression> =
        (paramList * whitespace * -Regex("->") * whitespace * ref { expression }) mapEx { parseCtx, result ->
            val (params, bodyParser) = result.value
            LambdaExpression(params, bodyParser, SourcePosition(result.start, result.end, result.text(parseCtx)))
        }

    private val exprList: Parser<List<Expression>> = run {
        val restItem = whitespace * -',' * whitespace * ref { expression }
        (ref { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    private val argList: Parser<List<Expression>> =
        -'(' * whitespace * (exprList + (whitespace map { emptyList<Expression>() })) * whitespace * -')'

    private val functionCall: Parser<Expression> =
        (identifier * whitespace * argList) mapEx { parseCtx, result ->
            val (name, args) = result.value
            FunctionCallExpression(name, args, SourcePosition(result.start, result.end, result.text(parseCtx)), parseCtx.src)
        }

    private val primary: Parser<Expression> =
        lambda + functionCall + variableRef + (number map { NumberLiteralExpression(it) }) +
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor = primary

    private val multiplyOp = (whitespace * +'*' * whitespace * factor) mapEx { parseCtx, result ->
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '*' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> MultiplyExpression(left, rightExpr, opPosition) }
    }

    private val divideOp = (whitespace * +'/' * whitespace * factor) mapEx { parseCtx, result ->
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '/' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> DivideExpression(left, rightExpr, opPosition) }
    }

    private val product: Parser<Expression> =
        leftAssociativeBinaryOp(factor, multiplyOp + divideOp)

    private val addOp = (whitespace * +'+' * whitespace * product) mapEx { parseCtx, result ->
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '+' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> AddExpression(left, rightExpr, opPosition) }
    }

    private val subtractOp = (whitespace * +'-' * whitespace * product) mapEx { parseCtx, result ->
        val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '-' }
        val (_, rightExpr: Expression) = result.value
        val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
        return@mapEx { left: Expression -> SubtractExpression(left, rightExpr, opPosition) }
    }

    private val sum: Parser<Expression> =
        leftAssociativeBinaryOp(product, addOp + subtractOp)

    private val orderingComparison: Parser<Expression> = run {
        val lessEqualOp = (whitespace * +"<=" * whitespace * sum) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("<=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> LessThanOrEqualExpression(left, rightExpr, opPosition) }
        }

        val greaterEqualOp = (whitespace * +">=" * whitespace * sum) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf(">=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> GreaterThanOrEqualExpression(left, rightExpr, opPosition) }
        }

        val lessOp = (whitespace * +'<' * whitespace * sum) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '<' }
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> LessThanExpression(left, rightExpr, opPosition) }
        }

        val greaterOp = (whitespace * +'>' * whitespace * sum) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOfFirst { it == '>' }
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> GreaterThanExpression(left, rightExpr, opPosition) }
        }

        val restItem = lessEqualOp + greaterEqualOp + lessOp + greaterOp

        leftAssociativeBinaryOp(sum, restItem)
    }

    private val equalityComparison: Parser<Expression> = run {
        val equalOp = (whitespace * +"==" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("==")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> EqualsExpression(left, rightExpr, opPosition) }
        }

        val notEqualOp = (whitespace * +"!=" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val opStart = result.start + parseCtx.src.substring(result.start, result.end).indexOf("!=")
            val (_, rightExpr: Expression) = result.value
            val opPosition = SourcePosition(opStart, result.end, result.text(parseCtx).trimStart())
            return@mapEx { left: Expression -> NotEqualsExpression(left, rightExpr, opPosition) }
        }

        val restItem = equalOp + notEqualOp

        leftAssociativeBinaryOp(orderingComparison, restItem)
    }

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

    private val indentFunctionDef: Parser<Expression> = Parser { context, start ->
        if (context !is IndentParseContext) return@Parser null

        val nameResult = identifier.parseOrNull(context, start) ?: return@Parser null
        val wsResult1 = whitespace.parseOrNull(context, nameResult.end) ?: return@Parser null
        val paramListResult = paramList.parseOrNull(context, wsResult1.end) ?: return@Parser null
        val wsResult2 = whitespace.parseOrNull(context, paramListResult.end) ?: return@Parser null

        if (context.src.getOrNull(wsResult2.end) != ':') return@Parser null
        var pos = wsResult2.end + 1

        val wsResult3 = (+Regex("[ \\t]*")).parseOrNull(context, pos) ?: return@Parser null
        pos = wsResult3.end

        if (context.src.getOrNull(pos) != '\n') return@Parser null
        pos++

        val indentSpaces = (+Regex("[ \\t]*")).parseOrNull(context, pos) ?: return@Parser null
        val indentLevel = indentSpaces.value.value.length

        if (indentLevel <= context.currentIndent) return@Parser null

        context.pushIndent(indentLevel)
        pos = indentSpaces.end

        try {
            val bodyResult = ref { expression }.parseOrNull(context, pos)
            if (bodyResult == null) {
                context.popIndent()
                return@Parser null
            }

            context.popIndent()

            val name = nameResult.value
            val params = paramListResult.value
            val body = bodyResult.value
            val lambda = LambdaExpression(params, body, SourcePosition(start, bodyResult.end, context.src.substring(start, bodyResult.end)))
            val assignment = AssignmentExpression(name, lambda)

            ParseResult(assignment, start, bodyResult.end)
        } catch (e: Exception) {
            context.popIndent()
            throw e
        }
    }

    private val assignment: Parser<Expression> = run {
        indentFunctionDef +
        ((identifier * whitespace * -'=' * whitespace * ref { expression }) map { (name, valueExpr) ->
            AssignmentExpression(name, valueExpr)
        }) + ternary
    }

    val expression: Parser<Expression> = assignment

    val program: Parser<Expression> = run {
        val newlineSep = -Regex("[ \\t]*\\r?\\n[ \\t\\r\\n]*")
        ((expression * (newlineSep * expression).zeroOrMore) map { (first, rest) ->
            ProgramExpression(listOf(first) + rest)
        })
    }

    val root = whitespace * expression * whitespace
    val programRoot = whitespace * program * whitespace
}

private fun formatParseException(e: ParseException, input: String): String {
    val sb = StringBuilder()

    val position = e.context.errorPosition

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

    sb.append("Error: Syntax error at line $line, column $column")

    if (e.context.suggestedParsers.isNotEmpty()) {
        val candidates = e.context.suggestedParsers
            .mapNotNull { it.name }
            .distinct()
        if (candidates.isNotEmpty()) {
            sb.append("\nExpected: ${candidates.joinToString(", ")}")
        }
    }

    val lineStart = beforePosition.lastIndexOf('\n') + 1
    val lineEnd = input.indexOf('\n', position).let { if (it == -1) input.length else it }
    val sourceLine = input.substring(lineStart, lineEnd)

    if (sourceLine.isNotEmpty()) {
        sb.append("\n")
        sb.append(sourceLine)
        sb.append("\n")
        val caretPosition = position - lineStart
        sb.append(" ".repeat(caretPosition.coerceAtLeast(0)))
        sb.append("^")
    }

    return sb.toString()
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        FunctionCallExpression.functionCallCount = 0

        val initialContext = EvaluationContext(sourceCode = input)

        val indentContext = IndentParseContext(input, useMemoization = true)
        val parseResult = indentContext.parseOrNull(ExpressionGrammar.programRoot, 0)

        if (parseResult == null || parseResult.end != input.length) {
            val resultExpr = ExpressionGrammar.programRoot.parseAllOrThrow(input)
            val result = resultExpr.evaluate(initialContext)
            result.toString()
        } else {
            val result = parseResult.value.evaluate(initialContext)
            result.toString()
        }
    } catch (e: EvaluationException) {
        if (e.context != null && e.context.callStack.isNotEmpty()) {
            e.formatWithCallStack()
        } else {
            "Error: ${e.message}"
        }
    } catch (e: ParseException) {
        formatParseException(e, input)
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
