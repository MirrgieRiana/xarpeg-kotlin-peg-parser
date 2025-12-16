package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.impl.escapeDoubleQuote
import io.github.mirrgieriana.xarpite.xarpeg.impl.truncate
import io.github.mirrgieriana.xarpite.xarpeg.parsers.normalize

fun interface Parser<out T : Any> {
    fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>?
    val name: String? get() = null
}

val Parser<*>.nameOrString get() = this.name ?: this.toString()

class ParseContext(val src: String, val useMemoization: Boolean) {
    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()
    var isInNamedParser = false
    var errorPosition: Int = 0
    val suggestedParsers = mutableSetOf<Parser<*>>()

    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                return memo[key] as ParseResult<T>?
            } else {
                val result = if (!isInNamedParser && parser.name != null) {
                    isInNamedParser = true
                    val result = parser.parseOrNull(this, start)
                    isInNamedParser = false
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
                val result = parser.parseOrNull(this, start)
                isInNamedParser = false
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
            suggestedParsers += parser
        }
        return result
    }
}

data class ParseResult<out T : Any>(val value: T, val start: Int, val end: Int)

fun ParseResult<*>.text(context: ParseContext) = context.src.substring(this.start, this.end).normalize()

open class ParseException(message: String, val context: ParseContext, val position: Int) : Exception(message)


class UnmatchedInputParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

class ExtraCharactersParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useMemoization: Boolean = true): T {
    val context = ParseContext(src, useMemoization)
    val result = this.parseOrNull(context, 0) ?: throw UnmatchedInputParseException("Failed to parse.", context, 0)
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        throw ExtraCharactersParseException("""Extra characters found after position ${result.end}: "$string"""", context, result.end)
    }
    return result.value
}
