package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncateWithCaret
import io.github.mirrgieriana.xarpeg.parsers.endOfInput
import io.github.mirrgieriana.xarpeg.parsers.normalize

// fun interfaceにすると1.9.21/jvmで不正なname-getterを持つクラスが生成されてバグる
interface Parser<out T : Any> {
    fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>?
    val name: String? get() = null
}

inline fun <T : Any> Parser(crossinline block: (context: ParseContext, start: Int) -> ParseResult<T>?): Parser<T> {
    return object : Parser<T> {
        override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
            return block(context, start)
        }
    }
}

val Parser<*>.nameOrString get() = this.name ?: this.toString()

data class ParseResult<out T : Any>(val value: T, val start: Int, val end: Int)

fun ParseResult<*>.text(context: ParseContext) = context.src.substring(this.start, this.end).normalize()


open class ParseException(val context: ParseContext, val position: Int = context.errorPosition ?: 0) : Exception(run {
    val matrixPosition = context.matrixPositionCalculator.getMatrixPosition(position)
    "Syntax Error at ${matrixPosition.row}:${matrixPosition.column}"
})

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
fun ParseException.formatMessage(maxLineLength: Int = 80): String {
    val suggestingParseContext = this.context as? SuggestingParseContext
    val matrixPositionCalculator = this.context.matrixPositionCalculator
    val sb = StringBuilder()


    // Build error message header with position
    val matrixPosition = matrixPositionCalculator.getMatrixPosition(this.position)
    sb.append("Syntax Error at ${matrixPosition.row}:${matrixPosition.column}")


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
        val actualChar = this.context.src.getOrNull(this.position)
        if (actualChar != null) {
            sb.append("\nActual: \"${actualChar.toString().escapeDoubleQuote()}\"")
        } else {
            sb.append("\nActual: EOF")
        }
    }


    // Add source line and caret
    if (suggestingParseContext != null) {
        val lineIndex = matrixPositionCalculator.getMatrixPosition(this.position).row - 1
        val lineRange = matrixPositionCalculator.getLineRange(lineIndex + 1)
        val lineStartIndex = lineRange.start
        val line = this.context.src.substring(lineStartIndex, lineRange.endInclusive + 1)

        val caretPosition = (this.position - lineStartIndex).coerceAtMost(line.length)
        val (displayLine, displayCaretPos) = line.truncateWithCaret(maxLineLength, caretPosition)

        sb.append("\n")

        sb.append(displayLine)
        sb.append("\n")

        sb.append(" ".repeat(displayCaretPos.coerceAtLeast(0)))
        sb.append("^")
    }


    return sb.toString()
}


fun <T : Any> Parser<T>.parseAll(
    src: String,
): Result<T> = this.parseAll(src) { DefaultParseContext(src) }

fun <T : Any> Parser<T>.parseAll(
    src: String,
    useMemoization: Boolean,
): Result<T> = this.parseAll(src) { s -> DefaultParseContext(s).also { it.useMemoization = useMemoization } }

fun <T : Any> Parser<T>.parseAllOrThrow(
    src: String,
    useMemoization: Boolean = true,
): T = this.parseAll(src, useMemoization).getOrThrow()

fun <T : Any, C : ParseContext> Parser<T>.parseAll(
    src: String,
    contextFactory: (String) -> C,
): Result<T> {
    val context = contextFactory(src)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(ParseException(context))
    context.parseOrNull(endOfInput, result.end) ?: return Result.failure(ParseException(context))
    return Result.success(result.value)
}
