package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseContext

/**
 * Custom ParseContext for the online parser.
 * This extends ParseContext to track the current indentation level for indent-based syntax.
 *
 * **Note:** This sample requires ParseContext to be `open class`.
 * After a release containing the `open class` change, update the version in
 * samples/libs.versions.toml to match the published version.
 */
class OnlineParserParseContext(
    src: String,
    useMemoization: Boolean = true,
) : ParseContext(src, useMemoization) {
    private val indentStack = mutableListOf(0)

    /**
     * Get the current required indent level
     */
    val currentIndent: Int
        get() = indentStack.last()

    /**
     * Push a new indent level onto the stack
     */
    fun pushIndent(indent: Int) {
        require(indent > currentIndent) { "New indent ($indent) must be greater than current indent ($currentIndent)" }
        indentStack.add(indent)
    }

    /**
     * Pop the current indent level from the stack
     */
    fun popIndent() {
        require(indentStack.size > 1) { "Cannot pop base indent level" }
        indentStack.removeLast()
    }
}
