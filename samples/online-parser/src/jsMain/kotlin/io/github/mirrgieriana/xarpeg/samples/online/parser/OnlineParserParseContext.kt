package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.DefaultParseContext

/**
 * Custom [DefaultParseContext] that tracks indentation levels and heredoc delimiter state.
 *
 * Maintains an immutable stack of indent levels and an optional heredoc delimiter.
 * [getState] returns both, enabling correct memoization when either state changes.
 */
class OnlineParserParseContext(src: String) : DefaultParseContext(src) {
    private var indentStack = listOf(0)

    /**
     * The delimiter of the heredoc currently being parsed, or `null` if not inside a heredoc.
     */
    var heredocDelimiter: String? = null

    override fun getState(): Any = Pair(indentStack, heredocDelimiter)

    /**
     * The current required indentation level (top of the stack).
     */
    val currentIndent: Int
        get() = indentStack.last()

    /**
     * Pushes a new indentation level onto the stack. Must be greater than [currentIndent].
     */
    fun pushIndent(indent: Int) {
        require(indent > currentIndent) { "New indent ($indent) must be greater than current indent ($currentIndent)" }
        indentStack = indentStack + indent
    }

    /**
     * Pops the top indentation level from the stack. Cannot pop the base level.
     */
    fun popIndent() {
        require(indentStack.size > 1) { "Cannot pop base indent level" }
        indentStack = indentStack.dropLast(1)
    }
}
