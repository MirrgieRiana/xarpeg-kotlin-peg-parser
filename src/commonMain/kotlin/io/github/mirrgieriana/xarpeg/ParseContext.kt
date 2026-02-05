package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.truncateWithCaret

class ParseContext(val src: String, val useMemoization: Boolean) {

    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    var isInNamedParser = false
    var errorPosition: Int = 0
    val suggestedParsers = mutableSetOf<Parser<*>>()

    private val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }
    fun toMatrixPosition(index: Int) = matrixPositionCalculator.toMatrixPosition(index)
    val errorMatrixPosition get() = toMatrixPosition(errorPosition)

    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                @Suppress("UNCHECKED_CAST")
                memo[key] as ParseResult<T>?
            } else {
                val result = if (!isInNamedParser && parser.name != null) {
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
                memo[key] = result
                result
            }
        } else {
            if (!isInNamedParser && parser.name != null) {
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
        if (result == null && !isInNamedParser && start >= errorPosition) {
            if (start > errorPosition) {
                errorPosition = start
                suggestedParsers.clear()
            }
            // Only add parsers with names to suggestions - unnamed parsers are just noise
            if (parser.name != null) {
                suggestedParsers += parser
            }
        }
        return result
    }

}

data class MatrixPosition(val row: Int, val column: Int)

class MatrixPositionCalculator(private val src: String) {
    private val lineStartIndices = run {
        val list = mutableListOf(0)
        src.forEachIndexed { index, char ->
            if (char == '\n') list.add(index + 1)
        }
        list
    }

    fun toMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }

    /**
     * Formats a parse error into a user-friendly error message with context.
     *
     * The formatted message includes:
     * - Error line and column number
     * - Expected parsers (if named parsers are available)
     * - The source line where the error occurred (truncated to maxLineLength if needed)
     * - A caret (^) indicating the error position
     *
     * @param position The error position in the source
     * @param suggestedParsers Set of parsers that failed at the error position
     * @param maxLineLength Maximum length for the source line display (default 80)
     * @return A formatted error message with context
     */
    fun formatMessage(position: Int, suggestedParsers: Set<Parser<*>>, maxLineLength: Int = 80): String {
        val sb = StringBuilder()

        val matrixPosition = toMatrixPosition(position)
        sb.append("Error: Syntax error at line ${matrixPosition.row}, column ${matrixPosition.column}")

        // Add expected parsers if available
        if (suggestedParsers.isNotEmpty()) {
            val candidates = suggestedParsers
                .mapNotNull { it.name }
                .distinct()
            if (candidates.isNotEmpty()) {
                sb.append("\nExpected: ${candidates.joinToString(", ")}")
            }
        }

        // Add source line and caret
        val lineIndex = lineStartIndices.binarySearch(position).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        val lineEnd = lineStartIndices.getOrNull(lineIndex + 1)?.let { it - 1 } ?: src.length
        val sourceLine = src.substring(lineStart, lineEnd)

        if (sourceLine.isNotEmpty()) {
            val caretPosition = position - lineStart
            val (displayLine, displayCaretPos) = sourceLine.truncateWithCaret(
                maxLineLength,
                caretPosition
            )

            sb.append("\n")
            sb.append(displayLine)
            sb.append("\n")
            sb.append(" ".repeat(displayCaretPos.coerceAtLeast(0)))
            sb.append("^")
        }

        return sb.toString()
    }
}
