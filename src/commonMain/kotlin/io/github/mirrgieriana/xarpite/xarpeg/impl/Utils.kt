package io.github.mirrgieriana.xarpite.xarpeg.impl

internal data class MatrixPosition(val row: Int, val column: Int)

internal fun createIndexToPositionConverter(src: String): (Int) -> MatrixPosition {
    val lineStartIndices = mutableListOf(0)
    src.forEachIndexed { index, char ->
        if (char == '\n') lineStartIndices.add(index + 1)
    }

    return { index ->
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }
}

internal fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    if (maxLength < 0) return ""
    if (this.length <= maxLength) return this
    if (maxLength <= ellipsis.length) return this.take(maxLength)
    return this.take(maxLength - ellipsis.length) + ellipsis
}

internal fun String.escapeJsonString(): String {
    val builder = StringBuilder(length)
    for (ch in this) {
        when (ch) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\b' -> builder.append("\\b")
            '\u000C' -> builder.append("\\f")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> {
                if (ch < ' ') {
                    builder.append("\\u")
                    val code = ch.code
                    builder.append(((code shr 12) and 0xF).toHexDigit())
                    builder.append(((code shr 8) and 0xF).toHexDigit())
                    builder.append(((code shr 4) and 0xF).toHexDigit())
                    builder.append((code and 0xF).toHexDigit())
                } else {
                    builder.append(ch)
                }
            }
        }
    }
    return builder.toString()
}

private fun Int.toHexDigit(): Char {
    val d = this and 0xF
    return if (d < 10) ('0'.code + d).toChar() else ('a'.code + (d - 10)).toChar()
}
