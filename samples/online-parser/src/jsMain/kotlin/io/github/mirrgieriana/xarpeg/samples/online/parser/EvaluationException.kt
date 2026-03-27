package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.MatrixPositionCalculator
import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * Exception thrown during expression evaluation, with call stack and source code for error reporting.
 */
class EvaluationException(
    message: String,
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    private val calculator by lazy { sourceCode?.let { MatrixPositionCalculator(it) } }

    /**
     * Formats the error message with a full call stack trace, showing source positions.
     */
    fun formatWithCallStack(): String {
        val sb = StringBuilder()
        sb.append("Error: $message")

        if (callStack.isNotEmpty()) {
            val calc = calculator
            callStack.asReversed().forEach { frame ->
                if (calc != null) {
                    sb.append(formatPositionHighlight(frame.functionName, frame.position, calc))
                } else {
                    sb.append("\n  at ${frame.functionName}, position ${frame.position.start}-${frame.position.end}")
                }
            }
        }

        return sb.toString()
    }

    private fun formatPositionHighlight(name: String, position: ParseResult<*>, calc: MatrixPositionCalculator): String {
        val pos = calc.getMatrixPosition(position.start.coerceAtMost(calc.src.length))

        val lineRange = calc.getLineRange(pos.row)
        val sourceLine = calc.src.substring(lineRange)

        val highlightStart = position.start - lineRange.first
        val highlightEnd = (position.end - lineRange.first).coerceAtMost(sourceLine.length)
        val caretCount = (highlightEnd - highlightStart).coerceAtLeast(1)

        return buildString {
            append("\n  at $name, line ${pos.row}, column ${pos.column}")
            append("\n    $sourceLine")
            append("\n    ${" ".repeat(highlightStart)}${"^".repeat(caretCount)}")
        }
    }
}
