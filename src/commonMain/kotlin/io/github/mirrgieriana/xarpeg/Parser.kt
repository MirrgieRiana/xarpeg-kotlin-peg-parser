package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.internal.escapeDoubleQuote
import io.github.mirrgieriana.xarpeg.internal.truncate
import io.github.mirrgieriana.xarpeg.parsers.normalize

/**
 * すべてのパーサーの基本インターフェース。
 *
 * パーサーは指定された位置で入力をマッチさせ、成功時に型付きの結果を生成します。
 *
 * @param T パース成功時に生成される値の型。
 */
// fun interfaceにすると1.9.21/jvmで不正なname-getterを持つクラスが生成されてバグる
interface Parser<out T : Any> {
    /**
     * 指定された位置で入力のパースを試みます。
     *
     * @param context 入力文字列とメモ化状態を含むパースコンテキスト。
     * @param start 入力文字列内の開始位置。
     * @return パース成功時は[ParseResult]、失敗時は`null`。
     */
    fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>?
    
    /**
     * このパーサーのオプション名。エラーメッセージで使用されます。
     *
     * 名前付きパーサーはパース失敗時の提案に表示され、ユーザーが期待される内容を理解するのに役立ちます。
     */
    val name: String? get() = null
}

/**
 * ラムダ関数からカスタムパーサーを作成します。
 *
 * @param T パーサーが生成する値の型。
 * @param block パースロジックを実装する関数。
 * @return 提供されたブロックに処理を委譲するパーサー。
 */
inline fun <T : Any> Parser(crossinline block: (context: ParseContext, start: Int) -> ParseResult<T>?): Parser<T> {
    return object : Parser<T> {
        override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
            return block(context, start)
        }
    }
}

/**
 * パーサーの名前が利用可能な場合はそれを返し、そうでない場合は文字列表現を返します。
 */
val Parser<*>.nameOrString get() = this.name ?: this.toString()

/**
 * パース成功の結果を表します。
 *
 * @param T パースされた値の型。
 * @param value パースされた値。
 * @param start マッチした入力の開始位置。
 * @param end マッチした入力の終了位置（排他的）。
 */
data class ParseResult<out T : Any>(val value: T, val start: Int, val end: Int)

/**
 * パース結果からマッチしたテキストを抽出します。
 *
 * @param context ソース文字列を含むパースコンテキスト。
 * @return マッチした部分文字列。パーサーのルールに従って正規化されます。
 */
fun ParseResult<*>.text(context: ParseContext) = context.src.substring(this.start, this.end).normalize()


/**
 * パース失敗の基底例外。
 *
 * @param message エラーメッセージ。
 * @param context エラーが発生したパースコンテキスト。
 * @param position パースが失敗した入力内の位置。
 */
open class ParseException(message: String, val context: ParseContext, val position: Int) : Exception(message)

/**
 * パーサーが入力とマッチできなかった場合にスローされる例外。
 *
 * @param message エラーメッセージ。
 * @param context エラーが発生したパースコンテキスト。
 * @param position パースが失敗した入力内の位置。
 */
class UnmatchedInputParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

/**
 * パース成功後に余分な文字が残っている場合にスローされる例外。
 *
 * [parseAll]がプレフィックスのマッチに成功したが、入力全体を消費しなかった場合に発生します。
 *
 * @param message エラーメッセージ。
 * @param context エラーが発生したパースコンテキスト。
 * @param position 余分な文字が始まる位置。
 */
class ExtraCharactersParseException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)


/**
 * 入力文字列全体をパースし、失敗時に例外をスローします。
 *
 * @param src パースする入力文字列。
 * @param useMemoization バックトラッキングのパフォーマンス向上のためにメモ化を有効にするかどうか。デフォルトは`true`。
 * @return パースされた値。
 * @throws UnmatchedInputParseException パースに失敗した場合。
 * @throws ExtraCharactersParseException パース後に余分な文字が残っている場合。
 */
fun <T : Any> Parser<T>.parseAllOrThrow(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrThrow()

/**
 * 入力文字列全体をパースし、失敗時に`null`を返します。
 *
 * @param src パースする入力文字列。
 * @param useMemoization バックトラッキングのパフォーマンス向上のためにメモ化を有効にするかどうか。デフォルトは`true`。
 * @return パースされた値、またはパース失敗時は`null`。
 */
fun <T : Any> Parser<T>.parseAllOrNull(src: String, useMemoization: Boolean = true) = this.parseAll(src, useMemoization).getOrNull()

/**
 * 入力文字列全体をパースし、[Result]を返します。
 *
 * この関数は以下を保証します：
 * 1. パーサーが入力の先頭から正常にマッチすること
 * 2. 入力全体が消費されること（余分な文字が残らないこと）
 *
 * @param src パースする入力文字列。
 * @param useMemoization バックトラッキングのパフォーマンス向上のためにメモ化を有効にするかどうか。デフォルトは`true`。
 * @return 成功時はパースされた値を含む[Result]、失敗時は[ParseException]。
 */
fun <T : Any> Parser<T>.parseAll(src: String, useMemoization: Boolean = true): Result<T> {
    val context = ParseContext(src, useMemoization)
    val result = context.parseOrNull(this, 0) ?: return Result.failure(UnmatchedInputParseException("Failed to parse.", context, 0))
    if (result.end != src.length) {
        val string = src.drop(result.end).truncate(10, "...").escapeDoubleQuote()
        return Result.failure(ExtraCharactersParseException("""Extra characters found after position ${result.end}: "$string"""", context, result.end))
    }
    return Result.success(result.value)
}
