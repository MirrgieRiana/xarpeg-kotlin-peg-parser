package io.github.mirrgieriana.xarpeg

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


open class ParseException(val context: ParseContext) : Exception(run {
    val errorPosition = context.errorPosition
    if (errorPosition != null) {
        val matrixPosition = context.matrixPositionCalculator.getMatrixPosition(errorPosition)
        "Syntax Error at ${matrixPosition.row}:${matrixPosition.column}"
    } else {
        "Syntax Error"
    }
})

/**
 * Formats a [ParseException] into a user-friendly error message with context.
 * @see [ParseContext.formatMessage]
 */
fun ParseException.formatMessage() = context.matrixPositionCalculator.formatMessage(this, 80)


fun <T : Any> Parser<T>.parseAll(
    src: String,
): Result<T> = this.parseAll(src) { DefaultParseContext(src) }

fun <T : Any, C : ParseContext> Parser<T>.parseAll(
    src: String,
    contextFactory: (String) -> C,
): Result<T> {
    val context = contextFactory(src)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(ParseException(context))
    context.parseOrNull(endOfInput, result.end) ?: return Result.failure(ParseException(context))
    return Result.success(result.value)
}
