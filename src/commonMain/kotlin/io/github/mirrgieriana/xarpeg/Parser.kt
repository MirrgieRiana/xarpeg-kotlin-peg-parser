package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncate
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


open class ParseException(message: String, val context: ParseContext, val position: Int) : Exception(message)

class UnmatchedInputParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

class ExtraCharactersParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)


fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrThrow()

fun <T : Any> Parser<T>.parseAllOrNull(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrNull()

fun <T : Any> Parser<T>.parseAll(src: String, useMemoization: Boolean = true): Result<T> {
    val context = ParseContext(src, useMemoization)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(UnmatchedInputParseException("Failed to parse.", context, 0))
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        return Result.failure(ExtraCharactersParseException("""Extra characters found after position ${result.end}: "$string"""", context, result.end))
    }
    return Result.success(result.value)
}

/**
 * Formats a ParseException into a user-friendly error message with context.
 *
 * The formatted message includes:
 * - Error line and column number
 * - Expected parsers (if named parsers are available)
 * - The source line where the error occurred
 * - A caret (^) indicating the error position
 *
 * Example output:
 * ```
 * Error: Syntax error at line 1, column 4
 * Expected: operator
 * 42 + 10
 *    ^
 * ```
 *
 * @param input The original input string that was being parsed
 * @return A formatted error message with context
 */
fun ParseException.formatMessage(input: String): String {
    // Use the errorPosition from context for UnmatchedInputParseException
    // and the position property for ExtraCharactersParseException
    val position = if (this is ExtraCharactersParseException) {
        this.position
    } else {
        this.context.errorPosition
    }

    val calculator = MatrixPositionCalculator(input)
    return calculator.formatMessage(position, this.context.suggestedParsers)
}


