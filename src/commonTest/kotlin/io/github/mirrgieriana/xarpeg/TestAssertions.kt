package io.github.mirrgieriana.xarpeg

import kotlin.test.fail

fun assertExtraCharacters(block: () -> Unit) {
    try {
        block()
        fail("Expected ParseException, but no exception was thrown.")
    } catch (_: ParseException) {
        // ok
    } catch (e: Throwable) {
        fail("Expected ParseException, but got ${e::class}", e)
    }
}

fun assertUnmatchedInput(block: () -> Unit) {
    try {
        block()
        fail("Expected ParseException, but no exception was thrown.")
    } catch (_: ParseException) {
        // ok
    } catch (e: Throwable) {
        fail("Expected ParseException, but got ${e::class}", e)
    }
}
