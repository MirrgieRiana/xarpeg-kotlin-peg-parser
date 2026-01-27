package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * ラップされたパーサーがマッチすれば成功するが、入力を消費しないパーサー。
 *
 * これは肯定先読みアサーションです。パース位置を進めることなく、現在位置でパターンが
 * マッチできることをチェックします。
 *
 * @param T ラップされたパーサーが生成する値の型。
 * @param parser チェックするパーサー。
 */
class LookAheadParser<T : Any>(val parser: Parser<T>) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        val result = context.parseOrNull(parser, start)
        if (result == null) return null
        return ParseResult(result.value, start, start)
    }
}

/**
 * 肯定先読みパーサーを作成します。
 *
 * このパーサーがマッチする場合にパーサーは成功しますが、入力を消費しません。
 */
val <T : Any> Parser<T>.lookAhead get() = LookAheadParser(this)
