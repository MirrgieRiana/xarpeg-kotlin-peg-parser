package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.not
import io.github.mirrgieriana.xarpeg.parsers.oneOrMore
import io.github.mirrgieriana.xarpeg.parsers.or
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
import io.github.mirrgieriana.xarpeg.samples.online.parser.statements.ExpressionStatement
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.FunctionCallExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.GreaterThanExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.GreaterThanOrEqualExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LambdaExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LessThanExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.LessThanOrEqualExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.MultiplyExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.NotEqualsExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.NumberLiteralExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.StatementsExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.SubtractExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.TernaryExpression
import io.github.mirrgieriana.xarpeg.samples.online.parser.statements.VariableDeclarationStatement
import io.github.mirrgieriana.xarpeg.samples.online.parser.expressions.VariableReferenceExpression

/**
 * PEG grammar for the online parser's expression language.
 *
 * Supports arithmetic, comparison, equality, ternary operators, lambda expressions,
 * function calls, variable assignment, and indent-based function definitions.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
internal object OnlineParserGrammar {

    // -- Spacing --

    /** Matches a single line break: `\r\n`, `\n`, or bare `\r`. */
    val lineBreak: Parser<Tuple0> = -Regex("""\r\n|[\r\n]""") named "line-break"

    /** Horizontal spacing: spaces and tabs, no line breaks. */
    val s: Parser<Tuple0> = -Regex("""[ \t]*""") named "whitespace"

    /**
     * Matches horizontal whitespace and verifies indentation after a line break.
     * Inside an indent block, requires at least [OnlineParserParseContext.currentIndent] spaces/tabs.
     * Outside an indent block, consumes any horizontal whitespace without restriction.
     */
    val indent: Parser<Tuple0> = Parser { context, pos ->
        val result = context.parseOrNull(s, pos)!!
        if (context is OnlineParserParseContext && result.end - pos < context.currentIndent) {
            return@Parser null
        }
        result
    }

    /** Whitespace that may span multiple lines. Line breaks require sufficient indentation in indent blocks. */
    val b: Parser<Tuple0> = -((s * lineBreak).oneOrMore * indent).zeroOrMore * s

    /** Whitespace that must contain at least one line break. Used as a statement separator. */
    val newline: Parser<Tuple0> = -((s * lineBreak).oneOrMore * indent).oneOrMore * s

    /** Matches a single identifier continuation character: letter, digit, or underscore. */
    val identifierPart: Parser<Tuple0> = -Regex("""[a-zA-Z0-9_]""")

    // -- Terminals --

    val identifier: Parser<String> = +Regex("""[a-zA-Z_][a-zA-Z0-9_]*""") map { it.value } named "identifier"
    val number: Parser<NumberValue> = +Regex("""[0-9]+(?:\.[0-9]+)?""") map { NumberValue(it.value.toDouble()) } named "number"

    // -- Atoms --

    val variableRef: Parser<Expression> = identifier.result map { result ->
        VariableReferenceExpression(result.value, result)
    }

    val identifierListRestItem: Parser<String> = s * -',' * b * identifier
    val identifierList: Parser<List<String>> = (identifier * identifierListRestItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }

    val paramList: Parser<List<String>> =
        -'(' * b * (identifierList + (b map { emptyList<String>() })) * b * -')'

    val lambda: Parser<Expression> =
        (paramList * s * -"->" * b * ref { expression }).result map { result ->
            val (params, body) = result.value
            LambdaExpression(params, body, result)
        }

    val exprListRestItem: Parser<Expression> = s * -',' * b * ref { expression }
    val exprList: Parser<List<Expression>> = (ref { expression } * exprListRestItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }

    val argList: Parser<List<Expression>> =
        -'(' * b * (exprList + (b map { emptyList<Expression>() })) * b * -')'

    val functionCall: Parser<Expression> =
        (identifier * s * argList).result map { result ->
            val (name, args) = result.value
            FunctionCallExpression(name, args, result)
        }

    val primary: Parser<Expression> = or(
        lambda,
        functionCall,
        variableRef,
        number.result map { NumberLiteralExpression(it.value, it) },
        -'(' * b * ref { expression } * b * -')',
    )

    val factor: Parser<Expression> = primary

    // -- Binary operators --

    val product: Parser<Expression> =
        leftAssociative(factor, s * (+'*' + +'/') * b) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                '*' -> MultiplyExpression(left, right, position)
                else -> DivideExpression(left, right, position)
            }
        }

    val sum: Parser<Expression> =
        leftAssociative(product, s * (+'+' + +'-') * b) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                '+' -> AddExpression(left, right, position)
                else -> SubtractExpression(left, right, position)
            }
        }

    val orderingComparison: Parser<Expression> =
        leftAssociative(sum, s * (+"<=" + +">=" + +"<" + +">") * b) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                "<=" -> LessThanOrEqualExpression(left, right, position)
                ">=" -> GreaterThanOrEqualExpression(left, right, position)
                "<" -> LessThanExpression(left, right, position)
                else -> GreaterThanExpression(left, right, position)
            }
        }

    val equalityComparison: Parser<Expression> =
        leftAssociative(orderingComparison, s * (+"==" + +"!=") * b) { left, op, right ->
            val position = ParseResult(Unit, left.position.start, right.position.end)
            when (op) {
                "==" -> EqualsExpression(left, right, position)
                else -> NotEqualsExpression(left, right, position)
            }
        }

    // -- Ternary --

    val ternaryExpr: Parser<Expression> =
        (ref { equalityComparison } * s * -'?' * b *
            ref { equalityComparison } * s * -':' * b *
            ref { equalityComparison }).result map { result ->
            val (cond, trueExpr, falseExpr) = result.value
            TernaryExpression(cond, trueExpr, falseExpr, result)
        }
    val ternary: Parser<Expression> = or(
        ternaryExpr,
        equalityComparison,
    )

    // -- Indent-based function definition --

    /**
     * Creates a parser that expects indented content.
     * Measures the indentation at the current position using [indentParser],
     * verifies it exceeds the current indent level, then parses [body] within that indent scope.
     */
    fun <T : Any> indented(indentParser: Parser<*>, body: Parser<T>): Parser<T> = Parser { context, start ->
        if (context !is OnlineParserParseContext) return@Parser null
        val indentResult = context.parseOrNull(indentParser, start) ?: return@Parser null
        val indentLevel = indentResult.end - indentResult.start
        if (indentLevel <= context.currentIndent) return@Parser null
        context.pushIndent(indentLevel)
        try {
            context.parseOrNull(body, indentResult.end)
        } finally {
            context.popIndent()
        }
    }

    val indentFunctionDef: Parser<Expression> =
        (identifier * s * paramList * s * -':' * s * lineBreak * indented(s, ref { expression })).result map { result ->
            val (name, params, body) = result.value
            AssignmentExpression(name, LambdaExpression(params, body, result), result)
        }

    // -- Top-level rules --

    val valueExpression: Parser<Expression> = ternary

    val expression: Parser<Expression> = or(
        indentFunctionDef,
        (identifier * s * -'=' * b * ref { expression }).result map { result ->
            val (name, valueExpr) = result.value
            AssignmentExpression(name, valueExpr, result)
        },
        valueExpression,
    )

    val varDeclaration: Parser<Statement> =
        -"var" * !identifierPart * s * identifier * s * -'=' * b * ref { expression } map { (name, value) ->
            VariableDeclarationStatement(name, value)
        }

    val statement: Parser<Statement> = or(
        varDeclaration,
        expression map { ExpressionStatement(it) },
    )

    val program: Parser<Expression> = (statement * (newline * statement).zeroOrMore).result map { result ->
        val (first, rest) = result.value
        StatementsExpression(listOf(first) + rest, result)
    }

    val root: Parser<Expression> = b * program * b
}
