package io.github.mirrgieriana.xarpeg.samples.online.parser.indent

import io.github.mirrgieriana.xarpeg.ExtraCharactersParseException
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
 * Example usage of the indent-based parser
 */
fun demonstrateIndentParsing() {
    // Example 1: Simple function with one statement
    val example1 = "fun hello:\n    world"
    val result1 = IndentParser.program.parseAllWithIndentOrThrow(example1)
    console.log("Example 1:", result1)

    // Example 2: Function with multiple statements
    val example2 = "fun test:\n    a\n    b\n    c"
    val result2 = IndentParser.program.parseAllWithIndentOrThrow(example2)
    console.log("Example 2:", result2)

    // Example 3: Multiple functions, one empty
    val example3 = "fun empty:\nfun another:\n    x"
    val result3 = IndentParser.program.parseAllWithIndentOrThrow(example3)
    console.log("Example 3:", result3)
}
