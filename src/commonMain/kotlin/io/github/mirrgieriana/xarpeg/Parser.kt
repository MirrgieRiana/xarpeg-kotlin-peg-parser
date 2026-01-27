package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncate
import io.github.mirrgieriana.xarpeg.parsers.normalize

/**
 * Core interface for all parsers.
 *
 * A parser attempts to match input at a given position and produces a typed result on success.
 *
 * @param T The type of value produced by successful parsing.
 */
// fun interfaceにすると1.9.21/jvmで不正なname-getterを持つクラスが生成されてバグる
interface Parser<out T : Any> {
    /**
     * Attempts to parse input at the specified position.
     *
     * @param context The parsing context containing the input string and memoization state.
     * @param start The starting position in the input string.
     * @return A [ParseResult] if parsing succeeds, or `null` if it fails.
     */
    fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>?
    
    /**
     * Optional name for this parser, used in error messages.
     *
     * Named parsers appear in parse failure suggestions to help users understand what was expected.
     */
    val name: String? get() = null
}

/**
 * Creates a custom parser from a lambda function.
 *
 * @param T The type of value produced by the parser.
 * @param block A function that implements the parsing logic.
 * @return A parser that delegates to the provided block.
 */
inline fun <T : Any> Parser(crossinline block: (context: ParseContext, start: Int) -> ParseResult<T>?): Parser<T> {
    return object : Parser<T> {
        override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
            return block(context, start)
        }
    }
}

/**
 * Returns the parser's name if available, otherwise its string representation.
 */
val Parser<*>.nameOrString get() = this.name ?: this.toString()

/**
 * Represents a successful parse result.
 *
 * @param T The type of the parsed value.
 * @param value The parsed value.
 * @param start The starting position of the matched input.
 * @param end The ending position of the matched input (exclusive).
 */
data class ParseResult<out T : Any>(val value: T, val start: Int, val end: Int)

/**
 * Extracts the matched text from the parse result.
 *
 * @param context The parsing context containing the source string.
 * @return The substring that was matched, normalized according to the parser's rules.
 */
fun ParseResult<*>.text(context: ParseContext) = context.src.substring(this.start, this.end).normalize()


/**
 * Base exception for parsing failures.
 *
 * @param message The error message.
 * @param context The parsing context where the error occurred.
 * @param position The position in the input where parsing failed.
 */
open class ParseException(message: String, val context: ParseContext, val position: Int) : Exception(message)

/**
 * Exception thrown when the parser fails to match the input.
 *
 * @param message The error message.
 * @param context The parsing context where the error occurred.
 * @param position The position in the input where parsing failed.
 */
class UnmatchedInputParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

/**
 * Exception thrown when extra characters remain after successful parsing.
 *
 * This occurs when [parseAll] successfully matches a prefix but doesn't consume the entire input.
 *
 * @param message The error message.
 * @param context The parsing context where the error occurred.
 * @param position The position where the extra characters begin.
 */
class ExtraCharactersParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)


/**
 * Parses the entire input string, throwing an exception on failure.
 *
 * @param src The input string to parse.
 * @param useMemoization Whether to enable memoization for better backtracking performance. Default is `true`.
 * @return The parsed value.
 * @throws UnmatchedInputParseException if parsing fails.
 * @throws ExtraCharactersParseException if extra characters remain after parsing.
 */
fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrThrow()

/**
 * Parses the entire input string, returning `null` on failure.
 *
 * @param src The input string to parse.
 * @param useMemoization Whether to enable memoization for better backtracking performance. Default is `true`.
 * @return The parsed value, or `null` if parsing fails.
 */
fun <T : Any> Parser<T>.parseAllOrNull(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrNull()

/**
 * Parses the entire input string, returning a [Result].
 *
 * This function ensures that:
 * 1. The parser successfully matches from the beginning of the input
 * 2. The entire input is consumed (no extra characters remain)
 *
 * @param src The input string to parse.
 * @param useMemoization Whether to enable memoization for better backtracking performance. Default is `true`.
 * @return A [Result] containing the parsed value on success, or a [ParseException] on failure.
 */
fun <T : Any> Parser<T>.parseAll(src: String, useMemoization: Boolean = true): Result<T> {
    val context = ParseContext(src, useMemoization)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(UnmatchedInputParseException("Failed to parse.", context, 0))
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        return Result.failure(ExtraCharactersParseException("""Extra characters found after position ${result.end}: "$string"""", context, result.end))
    }
    return Result.success(result.value)
}
