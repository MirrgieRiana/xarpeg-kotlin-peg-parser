package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.MatrixPositionCalculator
import io.github.mirrgieriana.xarpeg.ParseResult

/**
 * Exception thrown during expression evaluation, with optional call stack for error reporting.
 */
class EvaluationException(
    message: String,
    val context: Expression.EvaluationContext? = null,
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

        if (context != null && context.callStack.isNotEmpty()) {
            val calc = calculator
            context.callStack.asReversed().forEach { frame ->
                val location = if (calc != null) {
                    formatPositionHighlight(frame.position, calc)
                } else {
                    "position ${frame.position.start}-${frame.position.end}"
                }
                sb.append("\n  at $location")
            }
        }

        return sb.toString()
    }

    private fun formatPositionHighlight(position: ParseResult<*>, calc: MatrixPositionCalculator): String {
        val pos = calc.getMatrixPosition(position.start.coerceAtMost(calc.src.length))

        val lineRange = calc.getLineRange(pos.row)
        val sourceLine = calc.src.substring(lineRange)

        val highlightStart = position.start - lineRange.first
        val highlightEnd = (position.end - lineRange.first).coerceAtMost(sourceLine.length)

        val before = sourceLine.substring(0, highlightStart)
        val highlighted = sourceLine.substring(highlightStart, highlightEnd)
        val after = sourceLine.substring(highlightEnd)

        return "line ${pos.row}, column ${pos.column}: $before[$highlighted]$after"
    }
}
