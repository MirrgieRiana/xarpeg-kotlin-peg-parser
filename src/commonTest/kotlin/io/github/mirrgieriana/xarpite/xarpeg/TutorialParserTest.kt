package io.github.mirrgieriana.xarpite.xarpeg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParserTest {
    @Test
    fun parseSingleA() {
        val result = parseA("a")
        assertNotNull(result)
        assertEquals("a", result.value)
        assertEquals("", result.rest)
    }

    @Test
    fun parseMultipleA() {
        val parser = repeatParser(parseA)
        val result = parser("aaaa")

        assertNotNull(result)
        assertEquals(listOf("a", "a", "a", "a"), result.value)
        assertEquals("", result.rest)
    }

    @Test
    fun repeatAllowsZeroMatch() {
        val parser = repeatParser(parseA)
        val result = parser("")

        assertNotNull(result)
        assertEquals(emptyList<String>(), result.value)
        assertEquals("", result.rest)
    }

    @Test
    fun failWhenNoALeading() {
        assertNull(parseA("b"))
    }
}
