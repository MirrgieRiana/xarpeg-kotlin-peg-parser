package io.github.mirrgieriana.xarpeg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexToPositionConverterTest {

    @Test
    fun singleLine() {
        val converter = MatrixPositionCalculator("abc")

        assertEquals(MatrixPosition(1, 1), converter.toMatrixPosition(0))
        assertEquals(MatrixPosition(1, 3), converter.toMatrixPosition(2))
        assertEquals(MatrixPosition(1, 4), converter.toMatrixPosition(3))
    }

    @Test
    fun multiLine() {
        val converter = MatrixPositionCalculator("ab\ncde\nf")

        assertEquals(MatrixPosition(1, 1), converter.toMatrixPosition(0))
        assertEquals(MatrixPosition(1, 3), converter.toMatrixPosition(2)) // newline character
        assertEquals(MatrixPosition(2, 1), converter.toMatrixPosition(3))
        assertEquals(MatrixPosition(2, 4), converter.toMatrixPosition(6)) // second newline
        assertEquals(MatrixPosition(3, 1), converter.toMatrixPosition(7))
    }

    @Test
    fun windowsNewline() {
        val converter = MatrixPositionCalculator("a\r\nb")

        assertEquals(MatrixPosition(1, 1), converter.toMatrixPosition(0))
        assertEquals(MatrixPosition(1, 2), converter.toMatrixPosition(1)) // carriage return
        assertEquals(MatrixPosition(1, 3), converter.toMatrixPosition(2)) // line feed
        assertEquals(MatrixPosition(2, 1), converter.toMatrixPosition(3))
    }

    @Test
    fun outOfRangeThrows() {
        val converter = MatrixPositionCalculator("abc")

        assertFailsWith<IllegalArgumentException> { converter.toMatrixPosition(-1) }
        assertFailsWith<IllegalArgumentException> { converter.toMatrixPosition(4) }
    }
}
