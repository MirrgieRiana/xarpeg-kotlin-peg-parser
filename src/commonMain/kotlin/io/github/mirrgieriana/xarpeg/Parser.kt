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


open class ParseException(message: String, val context: ParseContext, val position: Int, val rawMessage: String = message) : Exception(message)

class UnmatchedInputParseException(rawMessage: String, context: ParseContext, position: Int) : ParseException("Failed to parse: $rawMessage", context, position, rawMessage)

class ExtraCharactersParseException(rawMessage: String, context: ParseContext, position: Int) : ParseException("Extra characters: $rawMessage", context, position, rawMessage)

/** Formats a ParseException into a user-friendly error message with context. */
fun ParseException.formatMessage() =
    context.formatMessage(this)


fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrThrow()

fun <T : Any> Parser<T>.parseAllOrNull(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrNull()

fun <T : Any> Parser<T>.parseAll(src: String, useMemoization: Boolean = true): Result<T> {
    val context = ParseContext(src, useMemoization)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(UnmatchedInputParseException("Unmatched input", context, context.errorPosition))
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        return Result.failure(ExtraCharactersParseException("""Found after position ${result.end}: "$string"""", context, result.end))
    }
    return Result.success(result.value)
}

