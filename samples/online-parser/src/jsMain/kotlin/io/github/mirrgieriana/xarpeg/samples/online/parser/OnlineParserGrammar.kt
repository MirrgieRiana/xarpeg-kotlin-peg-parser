package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.parsers.ignore
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.result
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.AddExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.AssignmentExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.DivideExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.EqualsExpression
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

/**
 * PEG grammar for the online parser's expression language.
 *
 * Supports arithmetic, comparison, equality, ternary operators, lambda expressions,
 * function calls, variable assignment, and indent-based function definitions.
 */
internal object OnlineParserGrammar {

    // -- Whitespace & indentation --

    /**
     * Matches a single newline: `\r\n`, `\n`, or bare `\r`.
     */
    private val newline = -Regex("\\r\\n|[\\r\\n]")

    /**
     * Matches the required indentation after a newline.
     * Inside an indent block, requires at least [OnlineParserParseContext.currentIndent] spaces/tabs.
     * Outside an indent block, succeeds without consuming input.
     */
    private val indent: Parser<Tuple0> = Parser { context, pos ->
        if (context !is OnlineParserParseContext || !context.isInIndentBlock) {
            return@Parser ParseResult(Tuple0, pos, pos)
        }
        var spaceEnd = pos
        while (spaceEnd < context.src.length && (context.src[spaceEnd] == ' ' || context.src[spaceEnd] == '\t')) {
            spaceEnd++
        }
        if (spaceEnd - pos < context.currentIndent) return@Parser null
        ParseResult(Tuple0, pos, spaceEnd)
    }

    /**
     * Matches a newline followed by the required indentation.
     */
    private val newlineAndIndent = newline * indent

    /**
     * Matches whitespace including newlines.
     * Within indent blocks, each newline must be followed by sufficient indentation.
     */
    private val whitespace = (-Regex("[ \t]*") * newlineAndIndent).zeroOrMore.ignore * -Regex("[ \t]*")

    /**
     * Matches horizontal whitespace only (spaces and tabs, no newlines).
     */
    private val horizontalSpace = +Regex("[ \\t]*")

    // -- Terminals --

    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value } named "identifier"

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { NumberValue(it.value.toDouble()) } named "number"

    // -- Atoms --

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
        (paramList * whitespace * -"->" * whitespace * ref { expression }).result map { result ->
            val (params, body) = result.value
            LambdaExpression(params, body, result)
        }

    private val exprList: Parser<List<Expression>> = run {
        val restItem = whitespace * -',' * whitespace * ref { expression }
        (ref { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    private val argList: Parser<List<Expression>> =
        -'(' * whitespace * (exprList + (whitespace map { emptyList<Expression>() })) * whitespace * -')'

    private val functionCall: Parser<Expression> =
        (identifier * whitespace * argList).result map { result ->
            val (name, args) = result.value
            FunctionCallExpression(name, args, result)
        }

    private val primary: Parser<Expression> =
        lambda + functionCall + variableRef +
            (number.result map { NumberLiteralExpression(it.value, it) }) +
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor = primary

    // -- Binary operators --

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

    // -- Ternary --

    private val ternary: Parser<Expression> = run {
        val ternaryExpr = ref { equalityComparison } * whitespace * -'?' * whitespace *
            ref { equalityComparison } * whitespace * -':' * whitespace *
            ref { equalityComparison }
        ((ternaryExpr.result map { result ->
            val (cond, trueExpr, falseExpr) = result.value
            TernaryExpression(cond, trueExpr, falseExpr, result)
        }) + equalityComparison)
    }

    // -- Indent-based function definition --

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
                LambdaExpression(paramListResult.value, bodyResult.value, wholePosition),
                wholePosition,
            ),
            start,
            bodyResult.end,
        )
    }

    // -- Top-level rules --

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
