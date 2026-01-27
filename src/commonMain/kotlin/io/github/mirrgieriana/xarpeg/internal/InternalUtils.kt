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
