package io.github.mirrgieriana.xarpeg

import kotlin.test.fail

fun assertParseException(block: () -> Unit) {
    try {
        block()
        fail("Expected ParseException, but no exception was thrown.")
    } catch (_: ParseException) {
        // ok
    } catch (e: Throwable) {
        fail("Expected ParseException, but got ${e::class}", e)
    }
}

@Deprecated("Use assertParseException instead", ReplaceWith("assertParseException(block)"))
fun assertExtraCharacters(block: () -> Unit) = assertParseException(block)

@Deprecated("Use assertParseException instead", ReplaceWith("assertParseException(block)"))
fun assertUnmatchedInput(block: () -> Unit) = assertParseException(block)
