package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.result
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserUtilityTest {

    @Test
    fun parseResultTextNormalizesNewlines() {
        val context = ParseContext("line1\r\nline2", useMemoization = true)
        val parser = +"line1\r\nline2"

        val result = parser.parseOrNull(context, 0)

        assertEquals("line1\nline2", result?.text(context))
    }

    @Test
    fun mapExProvidesContextAndResultRange() {
        val parser = (+"foo") mapEx { ctx, result ->
            "${result.text(ctx)}@${result.start}-${result.end}"
        }

        assertEquals("foo@0-3", parser.parseAllOrThrow("foo"))
    }

    @Test
    fun resultExtensionReturnsParseResult() {
        val parser = (+"bar").result

        val result = parser.parseAllOrThrow("bar")

        assertEquals("bar", result.value)
        assertEquals(0, result.start)
        assertEquals(3, result.end)
    }
}
