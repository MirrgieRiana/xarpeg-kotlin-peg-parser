package mirrg.xarpite

fun String.escapeJsonString(): String {
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
