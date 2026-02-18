package io.github.mirrgieriana.xarpeg.internal

import kotlin.math.min

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


private const val PREFIX = "... "
private const val SUFFIX = " ..."

private abstract class TruncateCase(val line: String, maxLength: Int, val prefix: String, val suffix: String) {
    val visibleLength = min(line.length, maxLength) - prefix.length - suffix.length
    abstract val visibleStart: Int
    val truncated by lazy { prefix + line.drop(visibleStart).take(visibleLength) + suffix }
}

internal fun String.truncateWithCaret(maxLength: Int, caretPosition: Int): Pair<String, Int> {
    val minLength = PREFIX.length + 1 + SUFFIX.length // "... ^ ..." = 9
    require(maxLength >= minLength) { "maxLength must be at least $minLength, but was $maxLength" }

    // maxLength = 10

    // 0123456789
    //      ^
    // 12345 1234
    val leftMaxVisibleLength = maxLength / 2 // 10 / 2 = 5
    val rightMaxVisibleLength = maxLength - 1 - leftMaxVisibleLength // 10 - 5 - 1 = 4

    val leftCharCount = caretPosition
    val rightCharCount = this.length - caretPosition - 1

    val case = when {

        // Pattern 1: No truncation needed
        leftCharCount + 1 + rightCharCount <= maxLength -> object : TruncateCase(this, maxLength, "", "") {
            override val visibleStart = 0
        }

        // Pattern 2: Caret is near the start, truncate right only
        leftCharCount <= leftMaxVisibleLength -> object : TruncateCase(this, maxLength, "", SUFFIX) {
            override val visibleStart = 0
        }

        // Pattern 3: Caret is near the end, truncate left only
        rightCharCount <= rightMaxVisibleLength -> object : TruncateCase(this, maxLength, PREFIX, "") {
            override val visibleStart by lazy { line.length - visibleLength }
        }

        // Pattern 4: Truncate both sides
        else -> object : TruncateCase(this, maxLength, PREFIX, SUFFIX) {
            override val visibleStart by lazy { caretPosition - visibleLength / 2 }
        }

    }

    return Pair(case.truncated, case.prefix.length + caretPosition - case.visibleStart)
}
