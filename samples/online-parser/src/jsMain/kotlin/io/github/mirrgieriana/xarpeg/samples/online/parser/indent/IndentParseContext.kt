package io.github.mirrgieriana.xarpeg.samples.online.parser.indent

import io.github.mirrgieriana.xarpeg.ParseContext

/**
 * Example of an indent-aware ParseContext.
 * This extends ParseContext to track the current indentation level.
 *
 * **Note:** This example demonstrates indent-based language support that requires
 * ParseContext to be declared as `open class`. This feature is available when using
 * a development build or will be available in future releases of xarpeg.
 *
 * To use this example with a local development build:
 * 1. Build and publish the main project to mavenLocal: `./gradlew publishToMavenLocal`
 * 2. Add `mavenLocal()` to repositories in this build.gradle.kts
 * 3. Update libs.versions.toml to use version "latest"
 *
 * Example usage for parsing indent-based languages like Python.
 */
class IndentParseContext(
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

    /**
     * Peek at what the next indent would be without modifying the stack
     */
    fun peekIndent(indent: Int): Boolean = indent > currentIndent
}
