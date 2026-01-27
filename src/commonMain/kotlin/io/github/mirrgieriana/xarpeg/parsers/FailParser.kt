package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 常に失敗するパーサー。
 *
 * 条件付きパースロジックのプレースホルダーやデフォルトケースとして便利です。
 */
object FailParser : Parser<Nothing> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<Nothing>? {
        return null
    }
}

/**
 * 常に失敗するパーサーを返します。
 */
val fail get() = FailParser
