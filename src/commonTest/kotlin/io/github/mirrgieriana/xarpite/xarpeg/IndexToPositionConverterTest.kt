package io.github.mirrgieriana.xarpite.xarpeg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexToPositionConverterTest {

    @Test
    fun singleLine() {
        val converter = createIndexToPositionConverter("abc")

        assertEquals(MatrixPosition(1, 1), converter(0))
        assertEquals(MatrixPosition(1, 3), converter(2))
        assertEquals(MatrixPosition(1, 4), converter(3))
    }

    @Test
    fun multiLine() {
        val converter = createIndexToPositionConverter("ab\ncde\nf")

        assertEquals(MatrixPosition(1, 1), converter(0))
        assertEquals(MatrixPosition(1, 3), converter(2)) // newline character
        assertEquals(MatrixPosition(2, 1), converter(3))
        assertEquals(MatrixPosition(2, 4), converter(6)) // second newline
        assertEquals(MatrixPosition(3, 1), converter(7))
    }

    @Test
    fun windowsNewline() {
        val converter = createIndexToPositionConverter("a\r\nb")

        assertEquals(MatrixPosition(1, 1), converter(0))
        assertEquals(MatrixPosition(1, 2), converter(1)) // carriage return
        assertEquals(MatrixPosition(1, 3), converter(2)) // line feed
        assertEquals(MatrixPosition(2, 1), converter(3))
    }

    @Test
    fun outOfRangeThrows() {
        val converter = createIndexToPositionConverter("abc")

        assertFailsWith<IllegalArgumentException> { converter(-1) }
        assertFailsWith<IllegalArgumentException> { converter(4) }
    }
}
