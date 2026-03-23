@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.MatrixPositionCalculator
import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.formatMessage
import io.github.mirrgieriana.xarpeg.parseAll
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.result
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import io.github.mirrgieriana.xarpeg.samples.online.parser.IndentParsers.newline
import io.github.mirrgieriana.xarpeg.samples.online.parser.IndentParsers.whitespace
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
    fun pushFrame(functionName: String, callPosition: ParseResult<*>) =
        copy(callStack = callStack + CallFrame(functionName, callPosition))

    fun withNewScope() = copy(variableTable = variableTable.createChild())
}

data class CallFrame(val functionName: String, val position: ParseResult<*>)

fun ParseResult<*>.formatWithContext(source: String): String {
    val calc = MatrixPositionCalculator(source)
    val pos = calc.getMatrixPosition(start.coerceAtMost(source.length))

    val lineRange = calc.getLineRange(pos.row)
    val sourceLine = source.substring(lineRange)

    val highlightStart = start - lineRange.first
    val highlightEnd = (end - lineRange.first).coerceAtMost(sourceLine.length)

    val before = sourceLine.substring(0, highlightStart)
    val highlighted = sourceLine.substring(highlightStart, highlightEnd)
    val after = sourceLine.substring(highlightEnd)

    return "line ${pos.row}, column ${pos.column}: $before[$highlighted]$after"
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
        val definitionPosition: ParseResult<*>? = null
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
                    "position ${frame.position.start}-${frame.position.end}"
                }
                sb.append("\n  at $location")
            }
        }

        return sb.toString()
    }
}

private object ExpressionGrammar {
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value } named "identifier"

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) } named "number"

    private val variableRef: Parser<Expression> = identifier.result map { result ->
        VariableReferenceExpression(result.value, result)
    }

    private val identifierList: Parser<List<String>> = run {
        val restItem = whitespace * -',' * whitespace * identifier
        (identifier * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    private val paramList: Parser<List<String>> =
        -'(' * whitespace * (identifierList + (whitespace map { emptyList<String>() })) * whitespace * -')'

    private val lambda: Parser<Expression> =
        (paramList * whitespace * -Regex("->") * whitespace * ref { expression }).result map { result ->
            val (params, bodyParser) = result.value
            LambdaExpression(params, bodyParser, result)
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
            FunctionCallExpression(name, args, result, parseCtx.src)
        }

    private val primary: Parser<Expression> =
        lambda + functionCall + variableRef + (number.result map { NumberLiteralExpression(it.value, it) }) +
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor = primary

    private val product: Parser<Expression> =
        leftAssociative(factor, whitespace * (+'*' + +'/') * whitespace) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                '*' -> MultiplyExpression(left, right, position)
                else -> DivideExpression(left, right, position)
            }
        }

    private val sum: Parser<Expression> =
        leftAssociative(product, whitespace * (+'+' + +'-') * whitespace) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                '+' -> AddExpression(left, right, position)
                else -> SubtractExpression(left, right, position)
            }
        }

    private val orderingComparison: Parser<Expression> =
        leftAssociative(sum, whitespace * (+"<=" + +">=" + +"<" + +">") * whitespace) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                "<=" -> LessThanOrEqualExpression(left, right, position)
                ">=" -> GreaterThanOrEqualExpression(left, right, position)
                "<" -> LessThanExpression(left, right, position)
                else -> GreaterThanExpression(left, right, position)
            }
        }

    private val equalityComparison: Parser<Expression> =
        leftAssociative(orderingComparison, whitespace * (+"==" + +"!=") * whitespace) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                "==" -> EqualsExpression(left, right, position)
                else -> NotEqualsExpression(left, right, position)
            }
        }

    private val ternary: Parser<Expression> = run {
        val ternaryExpr = ref { equalityComparison } * whitespace * -'?' * whitespace *
            ref { equalityComparison } * whitespace * -':' * whitespace *
            ref { equalityComparison }
        ((ternaryExpr.result map { result ->
            val (cond, trueExpr, falseExpr) = result.value
            TernaryExpression(cond, trueExpr, falseExpr, result)
        }) + equalityComparison)
    }

    private val horizontalSpace = +Regex("[ \\t]*")

    private val indentFunctionDef: Parser<Expression> = Parser { context, start ->
        if (context !is OnlineParserParseContext) return@Parser null

        val nameResult = context.parseOrNull(identifier, start) ?: return@Parser null
        val afterNameWs = context.parseOrNull(whitespace, nameResult.end)?.end ?: return@Parser null
        val paramListResult = context.parseOrNull(paramList, afterNameWs) ?: return@Parser null
        val afterParamsWs = context.parseOrNull(whitespace, paramListResult.end)?.end ?: return@Parser null

        if (context.src.getOrNull(afterParamsWs) != ':') return@Parser null

        val afterColonWs = context.parseOrNull(horizontalSpace, afterParamsWs + 1)?.end ?: return@Parser null
        val afterNl = context.parseOrNull(newline, afterColonWs)?.end ?: return@Parser null
        val indentResult = context.parseOrNull(horizontalSpace, afterNl) ?: return@Parser null
        val indentLevel = indentResult.end - indentResult.start

        if (indentLevel <= context.currentIndent) return@Parser null

        context.pushIndent(indentLevel)
        val bodyResult: ParseResult<Expression>?
        try {
            bodyResult = context.parseOrNull(ref { expression }, indentResult.end)
        } finally {
            context.popIndent()
        }

        bodyResult ?: return@Parser null

        val wholePosition = ParseResult(Unit, start, bodyResult.end)
        ParseResult(
            AssignmentExpression(
                nameResult.value,
                LambdaExpression(
                    paramListResult.value,
                    bodyResult.value,
                    wholePosition
                ),
                wholePosition
            ),
            start,
            bodyResult.end
        )
    }

    private val assignment: Parser<Expression> = run {
        indentFunctionDef +
            ((identifier * whitespace * -'=' * whitespace * ref { expression }).result map { result ->
                val (name, valueExpr) = result.value
                AssignmentExpression(name, valueExpr, result)
            }) + ternary
    }

    val expression: Parser<Expression> = assignment

    val program: Parser<Expression> = run {
        val newlineSep = -Regex("[ \\t]*(?:\\r\\n|[\\r\\n])[ \\t\\r\\n]*")
        ((expression * (newlineSep * expression).zeroOrMore).result map { result ->
            val (first, rest) = result.value
            ProgramExpression(listOf(first) + rest, result)
        })
    }

    val root = whitespace * expression * whitespace
    val programRoot = whitespace * program * whitespace
}


@JsExport
data class ExpressionResult(
    val success: Boolean,
    val output: String
)

@JsExport
fun parseExpression(input: String): ExpressionResult {
    return try {
        FunctionCallExpression.functionCallCount = 0

        val initialContext = EvaluationContext(sourceCode = input)
        val resultExpr = ExpressionGrammar.programRoot.parseAll(input) { OnlineParserParseContext(it) }.getOrThrow()
        val result = resultExpr.evaluate(initialContext)
        ExpressionResult(success = true, output = result.toString())
    } catch (e: EvaluationException) {
        val errorMessage = if (e.context != null && e.context.callStack.isNotEmpty()) {
            e.formatWithCallStack()
        } else {
            "Error: ${e.message}"
        }
        ExpressionResult(success = false, output = errorMessage)
    } catch (e: ParseException) {
        ExpressionResult(success = false, output = e.formatMessage())
    } catch (e: Exception) {
        ExpressionResult(success = false, output = "Error: ${e.message}")
    }
}
