package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncateWithCaret

interface ParseContext {
    val src: String
    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>?
}

interface MemoizationParseContext {
    var useMemoization: Boolean
}

interface MatrixPositionCalculatorHolderParseContext {
    val matrixPositionCalculator: MatrixPositionCalculator
}

val ParseContext.matrixPositionCalculator get() = (this as? MatrixPositionCalculatorHolderParseContext)?.matrixPositionCalculator ?: MatrixPositionCalculator(src)

interface LookAheadHolderParseContext {
    var isInLookAhead: Boolean
}

interface SuggestingParseContext {
    val errorPosition: Int
    val suggestedParsers: Set<Parser<*>>
}

val ParseContext.errorPosition get() = (this as? SuggestingParseContext)?.errorPosition
val ParseContext.suggestedParsers get() = (this as? SuggestingParseContext)?.suggestedParsers

open class DefaultParseContext(override val src: String) :
    ParseContext,
    MemoizationParseContext,
    MatrixPositionCalculatorHolderParseContext,
    LookAheadHolderParseContext,
    SuggestingParseContext {

    override var useMemoization: Boolean = true
    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    private var isInNamedParser = false
    override var isInLookAhead = false
    override var errorPosition: Int = 0
    override val suggestedParsers = mutableSetOf<Parser<*>>()

    override val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }

    override fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        fun parse(): ParseResult<T>? {
            return if (!isInNamedParser && parser.name != null) {
                isInNamedParser = true
                val result = try {
                    parser.parseOrNull(this, start)
                } finally {
                    isInNamedParser = false
                }
                result
            } else {
                parser.parseOrNull(this, start)
            }
        }

        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                @Suppress("UNCHECKED_CAST")
                memo[key] as ParseResult<T>?
            } else {
                val result = parse()
                memo[key] = result
                result
            }
        } else {
            parse()
        }
        if (result == null && !isInNamedParser && !isInLookAhead && start >= errorPosition) {
            if (start > errorPosition) {
                errorPosition = start
                suggestedParsers.clear()
            }
            // Only add parsers with non-empty names to suggestions - unnamed and hidden parsers are just noise
            if (parser.name != null && parser.name != "") {
                suggestedParsers += parser
            }
        }
        return result
    }

}

/**
 * Formats a parse error into a user-friendly error message with context.
 *
 * The formatted message includes:
 * - Error line and column number
 * - Expected parsers (if named parsers are available)
 * - Actual character found (or EOF)
 * - The source line where the error occurred (truncated to maxLineLength if needed)
 * - A caret (^) indicating the error position
 *
 * @param exception The parse exception to format
 * @param maxLineLength Maximum length for the source line display (default 80)
 * @return A formatted error message with context
 */
fun MatrixPositionCalculator.formatMessage(exception: ParseException, maxLineLength: Int = 80): String {
    val suggestingParseContext = exception.context as? SuggestingParseContext
    val sb = StringBuilder()


    // Build error message header with position
    if (suggestingParseContext != null) {
        val matrixPosition = this.getMatrixPosition(suggestingParseContext.errorPosition)
        sb.append("Syntax Error at ${matrixPosition.row}:${matrixPosition.column}")
    } else {
        sb.append("Syntax Error")
    }


    // Add expected parsers
    if (suggestingParseContext != null) {
        val candidates = suggestingParseContext.suggestedParsers.mapNotNull { it.name!!.ifEmpty { null } }.distinct()
        if (candidates.isNotEmpty()) {
            sb.append("\nExpect: ${candidates.joinToString(", ")}")
        } else {
            sb.append("\nExpect:")
        }
    }


    // Add actual character
    if (suggestingParseContext != null) {
        val actualChar = exception.context.src.getOrNull(suggestingParseContext.errorPosition)
        if (actualChar != null) {
            sb.append("\nActual: \"${actualChar.toString().escapeDoubleQuote()}\"")
        } else {
            sb.append("\nActual: EOF")
        }
    }


    // Add source line and caret
    if (suggestingParseContext != null) {
        val lineIndex = this.getMatrixPosition(suggestingParseContext.errorPosition).row - 1
        val lineRange = this.getLineRange(lineIndex + 1)
        val lineStartIndex = lineRange.start
        val line = this.src.substring(lineStartIndex, lineRange.endInclusive + 1)

        val caretPosition = (suggestingParseContext.errorPosition - lineStartIndex).coerceAtMost(line.length)
        val (displayLine, displayCaretPos) = line.truncateWithCaret(maxLineLength, caretPosition)

        sb.append("\n")

        sb.append(displayLine)
        sb.append("\n")

        sb.append(" ".repeat(displayCaretPos.coerceAtLeast(0)))
        sb.append("^")
    }


    return sb.toString()
}
