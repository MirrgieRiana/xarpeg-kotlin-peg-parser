package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.impl.escapeDoubleQuote
import io.github.mirrgieriana.xarpite.xarpeg.impl.truncate
import io.github.mirrgieriana.xarpite.xarpeg.parsers.normalize

// Changed from 'fun interface' to regular 'interface' to avoid Kotlin 1.9.x compiler bug
// that generates illegal method name "<get-name>" in JVM bytecode
interface Parser<out T : Any> {
    fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>?
    val name: String? get() = null
}

// Inline helper function for SAM conversion to maintain lambda syntax compatibility
inline fun <T : Any> Parser(crossinline block: (ParseContext, Int) -> ParseResult<T>?): Parser<T> {
    return object : Parser<T> {
        override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? = block(context, start)
    }
}

val Parser<*>.nameOrString get() = this.name ?: this.toString()

class ParseContext(val src: String, val useCache: Boolean) {
    private val cache = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()
    var isInNamedParser = false
    var errorPosition: Int = 0
    val suggestedParsers = mutableSetOf<Parser<*>>()

    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        val result = if (useCache) {
            val key = Pair(parser, start)
            if (key in cache) {
                return cache[key] as ParseResult<T>?
            } else {
                val result = if (!isInNamedParser && parser.name != null) {
                    isInNamedParser = true
                    val result = parser.parseOrNull(this, start)
                    isInNamedParser = false
                    result
                } else {
                    parser.parseOrNull(this, start)
                }
                cache[key] = result
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

open class ParseException(message: String, val position: Int) : Exception(message)


class UnmatchedInputParseException(message: String, position: Int) : ParseException(message, position)

class ExtraCharactersParseException(message: String, position: Int) : ParseException(message, position)

fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useCache: Boolean = true): T {
    val context = ParseContext(src, useCache)
    val result = this.parseOrNull(context, 0) ?: throw UnmatchedInputParseException("Failed to parse.", 0) // TODO 候補
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        throw ExtraCharactersParseException("""Extra characters found after position ${result.end}: "$string"""", result.end)
    }
    return result.value
}
