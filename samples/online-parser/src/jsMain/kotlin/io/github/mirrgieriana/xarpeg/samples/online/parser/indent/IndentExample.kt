@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpeg.samples.online.parser.indent

import io.github.mirrgieriana.xarpeg.ExtraCharactersParseException
import io.github.mirrgieriana.xarpeg.ParseException
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.UnmatchedInputParseException
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.oneOrMore
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Parse all input using IndentParseContext and throw an exception if parsing fails
 */
fun <T : Any> Parser<T>.parseAllWithIndentOrThrow(src: String, useMemoization: Boolean = true): T {
    val context = IndentParseContext(src, useMemoization)
    val result = context.parseOrNull(this, 0) ?: throw UnmatchedInputParseException("Failed to parse.", context, 0)
    if (result.end != src.length) {
        throw ExtraCharactersParseException(
            """Extra characters found after position ${result.end}""",
            context,
            result.end,
        )
    }
    return result.value
}

/**
 * Simple AST nodes for the indent-based language
 */
sealed interface Node

data class FunctionNode(val name: String, val body: List<Node>) : Node
data class ExpressionNode(val value: String) : Node

/**
 * Example parser for an indent-based language (similar to Python)
 */
object IndentParser {
    // Parse spaces (not including newlines)
    private val spaces = +Regex("[ \\t]*") map { it.value }

    // Parse a newline
    private val newline = +'\n'

    // Parse identifier
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") named "identifier" map { it.value }

    // Parse a simple expression (just identifiers for now)
    private val simpleExpr = identifier map { ExpressionNode(it) }

    /**
     * Create a parser that matches the current indent level
     */
    private fun indent(): Parser<String> = Parser { context, start ->
        if (context !is IndentParseContext) {
            error("IndentParser requires IndentParseContext")
        }
        val expectedIndent = context.currentIndent
        val spaceResult = spaces.parseOrNull(context, start) ?: return@Parser null

        val actualIndent = spaceResult.value.length
        if (actualIndent == expectedIndent) {
            spaceResult
        } else {
            null
        }
    }

    /**
     * Create a parser for an indented block
     */
    private fun indentedBlock(contentParser: Parser<Node>): Parser<List<Node>> = Parser { context, start ->
        if (context !is IndentParseContext) {
            error("IndentParser requires IndentParseContext")
        }

        // Parse newline after the colon
        val nlResult = newline.parseOrNull(context, start) ?: return@Parser null
        var pos = nlResult.end

        // Detect the indent of the first line in the block
        val firstSpaceResult = spaces.parseOrNull(context, pos) ?: return@Parser null
        val firstIndent = firstSpaceResult.value.length

        // Empty block case - if next line doesn't have greater indent
        if (firstIndent <= context.currentIndent) {
            // Return empty list for empty block
            return@Parser ParseResult(emptyList(), start, nlResult.end)
        }

        // Push the new indent level
        context.pushIndent(firstIndent)

        val nodes = mutableListOf<Node>()

        try {
            // Parse first line
            val indentResult = indent().parseOrNull(context, pos)
            if (indentResult != null) {
                pos = indentResult.end
                val nodeResult = contentParser.parseOrNull(context, pos)
                if (nodeResult != null) {
                    nodes.add(nodeResult.value)
                    pos = nodeResult.end
                } else {
                    context.popIndent()
                    return@Parser null
                }
            } else {
                context.popIndent()
                return@Parser null
            }

            // Parse subsequent lines
            while (true) {
                val nlRes = newline.parseOrNull(context, pos) ?: break
                val indRes = indent().parseOrNull(context, nlRes.end)
                if (indRes == null) break

                val nodeRes = contentParser.parseOrNull(context, indRes.end)
                if (nodeRes == null) break

                nodes.add(nodeRes.value)
                pos = nodeRes.end
            }

            context.popIndent()
            ParseResult(nodes, start, pos)
        } catch (e: Exception) {
            context.popIndent()
            throw e
        }
    }

    // Parse a function definition
    private val functionDef: Parser<FunctionNode> =
        (+"fun" * +' ' * identifier * -':' * indentedBlock(simpleExpr)) map { (_, _, name, body) ->
            FunctionNode(name, body)
        } named "function"

    // Root parser
    val program: Parser<List<FunctionNode>> = functionDef.oneOrMore named "program"
}

/**
 * Parse and format indent-based code
 * This is exported for use in the online parser demo
 */
@JsExport
fun parseIndentCode(input: String): String {
    return try {
        val result = IndentParser.program.parseAllWithIndentOrThrow(input)
        result.joinToString("\n") { func ->
            val bodyStr = if (func.body.isEmpty()) {
                "  (empty)"
            } else {
                func.body.joinToString("\n") { "  - ${(it as ExpressionNode).value}" }
            }
            "Function '${func.name}':\n$bodyStr"
        }
    } catch (e: ParseException) {
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
        
        val sb = StringBuilder()
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
        
        sb.toString()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
