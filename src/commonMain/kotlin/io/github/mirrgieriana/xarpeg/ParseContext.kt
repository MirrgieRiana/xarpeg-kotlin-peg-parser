package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncateWithCaret

class ParseContext(val src: String, val useMemoization: Boolean) {

    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    var isInNamedParser = false
    var isInLookAhead = false
    var errorPosition: Int = 0
    val suggestedParsers = mutableSetOf<Parser<*>>()

    val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }

    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
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

    /**
     * Formats a [ParseException] into a user-friendly error message.
     * @see [MatrixPositionCalculator.formatMessage]
     */
    fun formatMessage(exception: ParseException, maxLineLength: Int) = matrixPositionCalculator.formatMessage(exception, maxLineLength)

}

data class MatrixPosition(val row: Int, val column: Int)

class MatrixPositionCalculator(val src: String) {
    private val lineStartIndices = mutableListOf<Int>()
    private val lineExclusiveEndIndices = mutableListOf<Int>()

    init {
        lineStartIndices += 0
        var result = """\n|\r\n?""".toRegex().find(src)
        while (result != null) {
            lineExclusiveEndIndices += result.range.first
            lineStartIndices += result.range.last + 1
            result = result.next()
        }
        lineExclusiveEndIndices += src.length
    }

    fun getLineRange(lineNumber: Int): IntRange {
        require(lineNumber in 1..lineStartIndices.size) { "lineNumber ($lineNumber) is out of range (1..${lineStartIndices.size})" }
        val lineIndex = lineNumber - 1
        return lineStartIndices[lineIndex] until lineExclusiveEndIndices[lineIndex]
    }

    fun getMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }
        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else (-it - 1) - 1 }
        val lineStartIndex = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStartIndex + 1)
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
    fun formatMessage(exception: ParseException, maxLineLength: Int = 80): String {
        val sb = StringBuilder()
        val matrixPosition = getMatrixPosition(exception.position)


        // Build error message header with position
        sb.append("Syntax Error at ${matrixPosition.row}:${matrixPosition.column}")


        // Add expected parsers
        val candidates = exception.context.suggestedParsers.mapNotNull { it.name!!.ifEmpty { null } }.distinct()
        if (candidates.isNotEmpty()) {
            sb.append("\nExpect: ${candidates.joinToString(", ")}")
        } else {
            sb.append("\nExpect:")
        }


        // Add actual character
        val actualChar = exception.context.src.getOrNull(exception.position)
        if (actualChar != null) {
            sb.append("\nActual: \"${actualChar.toString().escapeDoubleQuote()}\"")
        } else {
            sb.append("\nActual: EOF")
        }


        // Add source line and caret
        val lineIndex = matrixPosition.row - 1
        val lineStartIndex = lineStartIndices[lineIndex]
        val lineExclusiveEndIndex = lineExclusiveEndIndices[lineIndex]
        val line = src.substring(lineStartIndex, lineExclusiveEndIndex)

        val caretPosition = (exception.position - lineStartIndex).coerceAtMost(line.length)
        val (displayLine, displayCaretPos) = line.truncateWithCaret(maxLineLength, caretPosition)

        sb.append("\n")

        sb.append(displayLine)
        sb.append("\n")

        sb.append(" ".repeat(displayCaretPos.coerceAtLeast(0)))
        sb.append("^")


        return sb.toString()
    }
}
