package io.github.mirrgieriana.xarpeg.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class InternalUtilsTest {

    @Test
    fun truncateWithCaret_noTruncation() {
        val line = "0123456789"
        val (result, caretPos) = line.truncateWithCaret(10, 5)
        assertEquals("0123456789", result)
        assertEquals(5, caretPos)
    }

    @Test
    fun truncateWithCaret_rightTruncation() {
        // Caret at position 2, line is 15 chars, max is 10
        // Left side: 2 chars before caret (0-1), caret itself at 2
        // Should show: "012345 ..." (6 + 4 = 10 chars)
        val line = "012345678901234"
        val (result, caretPos) = line.truncateWithCaret(10, 2)
        assertEquals("012345 ...", result)
        assertEquals(2, caretPos)
    }

    @Test
    fun truncateWithCaret_leftTruncation() {
        // Caret at position 12, line is 15 chars, max is 10
        // charsAfterCaret = 2, rightMaxChars = 4
        // Pattern 3: left truncation
        // contentLength = 6, startIndex = 9
        // substring(9) = "901234"
        val line = "012345678901234"
        val (result, caretPos) = line.truncateWithCaret(10, 12)
        assertEquals("... 901234", result)
        assertEquals(7, caretPos) // "... " (4) + 3 chars to caret = 7
    }

    @Test
    fun truncateWithCaret_bothSidesTruncation() {
        // Caret at position 7, line is 20 chars, max is 10
        // Available chars: 10 - 8 = 2 chars (8 for "... " and " ...")
        // Left: 1 char, Right: 1 char
        // Should show: "... 67 ..." (4 + 2 + 4 = 10 chars)
        val line = "01234567890123456789"
        val (result, caretPos) = line.truncateWithCaret(10, 10)
        assertEquals("... 90 ...", result)
        assertEquals(5, caretPos) // "... " = 4 chars, then 1 char to caret
    }

    @Test
    fun truncateWithCaret_exactlyAtLeftBoundary() {
        // maxLength = 10, leftMaxChars = 5
        // Caret at position 5 should trigger right truncation
        val line = "012345678901234"
        val (result, caretPos) = line.truncateWithCaret(10, 5)
        assertEquals("012345 ...", result)
        assertEquals(5, caretPos)
    }

    @Test
    fun truncateWithCaret_exactlyAtRightBoundary() {
        // maxLength = 10, leftMaxChars = 5, rightMaxChars = 4
        // Line length 15, caret at 10: charsAfterCaret = 4
        // 4 <= 4, so Pattern 3: left truncation
        // contentLength = 6, startIndex = 9
        // substring(9) = "901234"
        val line = "012345678901234"
        val (result, caretPos) = line.truncateWithCaret(10, 10)
        assertEquals("... 901234", result)
        assertEquals(5, caretPos) // "... " (4) + 1 char to caret = 5
    }

    @Test
    fun truncateWithCaret_veryLongLine() {
        val line = "a".repeat(100)
        val (result, caretPos) = line.truncateWithCaret(10, 50)
        assertEquals(10, result.length)
        check(result.contains("..."))
    }

    @Test
    fun truncateWithCaret_caretAtStart() {
        val line = "0123456789abcdefghij"
        val (result, caretPos) = line.truncateWithCaret(10, 0)
        assertEquals("012345 ...", result)
        assertEquals(0, caretPos)
    }

    @Test
    fun truncateWithCaret_caretAtEnd() {
        // Line length 20, caret at position 19 (last char), maxLength 10
        // charsAfterCaret = 0, rightMaxChars = 4
        // Pattern 3: left truncation
        // contentLength = 6, startIndex = 14
        // substring(14) = "efghij"
        val line = "0123456789abcdefghij"
        val (result, caretPos) = line.truncateWithCaret(10, 19)
        assertEquals("... efghij", result)
        assertEquals(9, caretPos) // "... " (4) + 5 chars to caret = 9
    }
}
