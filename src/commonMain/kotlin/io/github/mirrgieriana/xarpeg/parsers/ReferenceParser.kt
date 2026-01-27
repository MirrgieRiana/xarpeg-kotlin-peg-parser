package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * A parser that lazily references another parser.
 *
 * This is essential for recursive grammars where a parser needs to reference itself or other
 * parsers that haven't been defined yet.
 *
 * @param T The type of value produced by the referenced parser.
 * @param parserGetter A function that returns the parser to delegate to.
 */
class ReferenceParser<out T : Any>(val parserGetter: () -> Parser<T>) : Parser<T> {
    private val parser by lazy { parserGetter() }
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

/**
 * Creates a lazy reference to a parser.
 *
 * This is crucial for recursive grammars. Example:
 * ```
 * val expr: Parser<Int> = object {
 *     val number = +Regex("[0-9]+") map { it.value.toInt() }
 *     val parens = -'(' * ref { expr } * -')'  // Forward reference
 *     val expr = number + parens
 * }.expr
 * ```
 */
fun <T : Any> ref(getter: () -> Parser<T>) = ReferenceParser(getter)
