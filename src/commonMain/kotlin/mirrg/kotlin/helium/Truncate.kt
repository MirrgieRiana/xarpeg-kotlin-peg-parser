package mirrg.kotlin.helium

fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    if (maxLength < 0) return ""
    if (this.length <= maxLength) return this
    if (maxLength <= ellipsis.length) return this.take(maxLength)
    return this.take(maxLength - ellipsis.length) + ellipsis
}
