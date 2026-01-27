package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * Parser that matches a repeated pattern.
 *
 * @param T The type of value produced by each match.
 * @param parser The parser to repeat.
 * @param min The minimum number of matches required (inclusive).
 * @param max The maximum number of matches allowed (inclusive).
 */
class ListParser<out T : Any>(val parser: Parser<T>, val min: Int, val max: Int) : Parser<List<T>> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<List<T>>? {
        val results = mutableListOf<T>()
        var nextIndex = start
        while (true) {
            val result = context.parseOrNull(parser, nextIndex) ?: break
            results += result.value
            nextIndex = result.end
            if (results.size >= max) break
        }
        if (results.size < min) return null
        return ParseResult(results, start, nextIndex)
    }
}

/**
 * Creates a parser that matches this parser repeatedly.
 *
 * @param min The minimum number of matches required. Default is 0.
 * @param max The maximum number of matches allowed. Default is [Int.MAX_VALUE].
 */
fun <T : Any> Parser<T>.list(min: Int = 0, max: Int = Int.MAX_VALUE) = ListParser(this, min, max)

/**
 * Creates a parser that matches this parser zero or more times (Kleene star).
 */
val <T : Any> Parser<T>.zeroOrMore get() = this.list()

/**
 * Creates a parser that matches this parser one or more times (Kleene plus).
 */
val <T : Any> Parser<T>.oneOrMore get() = this.list(min = 1)
