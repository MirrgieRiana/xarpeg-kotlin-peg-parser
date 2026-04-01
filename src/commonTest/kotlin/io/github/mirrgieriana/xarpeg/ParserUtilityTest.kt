package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.result
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParserUtilityTest {

    @Test
    fun parseResultTextNormalizesNewlines() {
        val context = DefaultParseContext("line1\r\nline2")
        val parser = +"line1\r\nline2"

        val result = parser.parseOrNull(context, 0)

        assertEquals("line1\nline2", result?.text(context))
    }

    @Test
    fun mapExProvidesContextAndResultRange() {
        val parser = (+"foo") mapEx { ctx, result ->
            "${result.text(ctx)}@${result.start}-${result.end}"
        }

        assertEquals("foo@0-3", parser.parseAll("foo").getOrThrow())
    }

    @Test
    fun resultExtensionReturnsParseResult() {
        val parser = (+"bar").result

        val result = parser.parseAll("bar").getOrThrow()

        assertEquals("bar", result.value)
        assertEquals(0, result.start)
        assertEquals(3, result.end)
    }

    @Test
    fun mapReturningNullCausesParseFailure() {
        val parser = +"abc" map { _: String -> null }
        val context = DefaultParseContext("abc")

        assertNull(parser.parseOrNull(context, 0))
    }

    @Test
    fun mapReturningNonNullSucceeds() {
        val parser = +"abc" map { value: String -> value.uppercase() }

        assertEquals("ABC", parser.parseAll("abc").getOrThrow())
    }

    @Test
    fun mapReturningNullWithChoiceFallsThrough() {
        val selective = +"abc" map { value: String ->
            if (value == "abc") null else value
        }
        val fallback = +"abc" map { "fallback" }
        val parser = selective + fallback

        assertEquals("fallback", parser.parseAll("abc").getOrThrow())
    }

    @Test
    fun mapExReturningNullCausesParseFailure() {
        val parser = +"xyz" mapEx { _, _ -> null }
        val context = DefaultParseContext("xyz")

        assertNull(parser.parseOrNull(context, 0))
    }

    @Test
    fun mapExReturningNonNullSucceeds() {
        val parser = (+"xyz") mapEx { ctx, result ->
            "${result.text(ctx)}!"
        }

        assertEquals("xyz!", parser.parseAll("xyz").getOrThrow())
    }

    @Test
    fun mapExReturningNullWithChoiceFallsThrough() {
        val selective = +"xyz" mapEx { _, _ -> null }
        val fallback = +"xyz" map { "fallback" }
        val parser = selective + fallback

        assertEquals("fallback", parser.parseAll("xyz").getOrThrow())
    }

    @Test
    fun mapNullPreservesStartEnd() {
        val parser = +"ab" map { value: String ->
            if (value == "ab") null else value.uppercase()
        }
        val context = DefaultParseContext("abcd")

        val result = parser.parseOrNull(context, 0)

        assertNull(result)
    }

    @Test
    fun mapExNullAtNonZeroOffset() {
        val parser = (+"cd") mapEx { _, result ->
            if (result.start == 2) null else result.value
        }
        val context = DefaultParseContext("xxcdyy")

        assertNull(parser.parseOrNull(context, 2))

        val context2 = DefaultParseContext("cdyy")
        val result = parser.parseOrNull(context2, 0)
        assertNotNull(result)
        assertEquals("cd", result.value)
    }
}
