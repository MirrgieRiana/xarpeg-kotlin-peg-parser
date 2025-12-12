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
                    builder.append("\\u%04x".format(ch.code))
                } else {
                    builder.append(ch)
                }
            }
        }
    }
    return builder.toString()
}
