package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * パーサーを人間が読みやすい名前でラップします。
 *
 * 名前はエラーメッセージに表示され、ユーザーがパース失敗時に期待されていた内容を
 * 理解するのに役立ちます。
 *
 * @param T ラップされたパーサーが生成する値の型。
 * @param parser ラップするパーサー。
 * @param name このパーサーに割り当てる名前。
 */
class NamedParser<T : Any>(val parser: Parser<T>, override val name: String) : Parser<T> {
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}

/**
 * より良いエラーメッセージのためにこのパーサーに名前を割り当てます。
 *
 * 例: `+Regex("[0-9]+") named "number"`はエラーメッセージに"number"を表示します。
 */
infix fun <T : Any> Parser<T>.named(name: String) = NamedParser(this, name)
