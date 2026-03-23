package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.DefaultParseContext

/**
 * Custom [DefaultParseContext] that tracks indentation levels for indent-based syntax.
 *
 * Maintains an immutable stack of indent levels. [getState] returns the stack directly,
 * enabling correct memoization when indentation state changes.
 */
class OnlineParserParseContext(src: String) : DefaultParseContext(src) {
    private var indentStack = listOf(0)

    override fun getState(): Any = indentStack

    /**
     * The current required indentation level (top of the stack).
     */
    val currentIndent: Int
        get() = indentStack.last()

    /**
     * Whether parsing is currently inside an indent block (stack depth > 1).
     */
    val isInIndentBlock: Boolean
        get() = indentStack.size > 1

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
