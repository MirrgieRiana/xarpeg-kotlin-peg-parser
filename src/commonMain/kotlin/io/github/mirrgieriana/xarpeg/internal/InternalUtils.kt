package io.github.mirrgieriana.xarpeg.internal

internal fun String.truncate(maxLength: Int, ellipsis: String): String {
    if (maxLength < 0) return ""
    if (this.length <= maxLength) return this
    return this.take(maxLength - ellipsis.length) + ellipsis
}

internal fun String.escapeDoubleQuote(): String {
    val sb = StringBuilder()
    this.forEach { char ->
        when {
            char == '\\' -> sb.append("\\\\")
            char == '"' -> sb.append("\\\"")
            char == '\n' -> sb.append("\\n")
            char == '\r' -> sb.append("\\r")
            char == '\t' -> sb.append("\\t")
            char.isISOControl() -> sb.appendUnicodeChar(char)
            else -> sb.append("$char")
        }
    }
    return sb.toString()
}

private fun StringBuilder.appendUnicodeChar(char: Char) {
    this.append("\\u")
    val code = char.code
    this.append(((code shr 12) and 0xF).toHexDigit())
    this.append(((code shr 8) and 0xF).toHexDigit())
    this.append(((code shr 4) and 0xF).toHexDigit())
    this.append((code and 0xF).toHexDigit())
}

private fun Int.toHexDigit() = if (this < 10) '0' + this else 'A' + (this - 10)

/**
 * Truncates a string to fit within a maximum length with a caret position indicator.
 *
 * When the line is longer than maxLength, it will be truncated with "..." on one or both sides,
 * keeping the caret position visible.
 *
 * There are four patterns:
 * 1. Line is <= maxLength: no truncation
 * 2. Caret position <= leftMaxChars: truncate right side only
 * 3. Remaining chars from caret <= rightMaxChars: truncate left side only
 * 4. Otherwise: truncate both sides
 *
 * When truncating, " ..." (4 chars) is added, so to save 1 char, 5 chars must be removed.
 *
 * @param maxLength Maximum length of the output string
 * @param caretPosition Position of the caret (0-indexed)
 * @return A pair of (truncated string, new caret position)
 */
internal fun String.truncateWithCaret(maxLength: Int, caretPosition: Int): Pair<String, Int> {
    if (this.length <= maxLength) {
        return Pair(this, caretPosition)
    }

    // leftMaxChars: Maximum characters to show on the left side when doing single-side truncation
    // rightMaxChars: Maximum characters to show on the right side of caret when doing single-side truncation
    val leftMaxChars = maxLength / 2
    val rightMaxChars = maxLength - leftMaxChars - 1 // -1 accounts for the caret character itself

    val charsAfterCaret = this.length - caretPosition - 1

    return when {
        // Pattern 2: Caret is near the start, truncate right only
        caretPosition <= leftMaxChars -> {
            val contentLength = maxLength - 4 // " ..." = 4 chars
            val truncated = this.substring(0, contentLength) + " ..."
            Pair(truncated, caretPosition)
        }
        // Pattern 3: Caret is near the end, truncate left only
        charsAfterCaret <= rightMaxChars -> {
            val contentLength = maxLength - 4 // "... " = 4 chars
            val startIndex = this.length - contentLength
            val truncated = "... " + this.substring(startIndex)
            val newCaretPos = caretPosition - startIndex + 4
            Pair(truncated, newCaretPos)
        }
        // Pattern 4: Truncate both sides
        else -> {
            // Total available chars for content: maxLength - 8 (for "... " and " ...")
            // Content structure: leftChars + caret (1 char) + rightChars
            val totalContentChars = maxLength - 8
            val leftChars = totalContentChars / 2
            val rightChars = totalContentChars - leftChars - 1 // -1 accounts for the caret character

            val leftStart = caretPosition - leftChars
            val rightEnd = caretPosition + 1 + rightChars

            val truncated = "... " + this.substring(leftStart, rightEnd) + " ..."
            val newCaretPos = 4 + leftChars
            Pair(truncated, newCaretPos)
        }
    }
}
