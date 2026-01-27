package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser

/**
 * 与えられた関数を使用してこのパーサーの結果を変換します。
 *
 * @param I このパーサーが生成する入力型。
 * @param O 変換が生成する出力型。
 * @param function 成功したパース結果に適用する変換関数。
 * @return 変換された値を生成するパーサー。
 */
infix fun <I : Any, O : Any> Parser<I>.map(function: (I) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(result.value), result.start, result.end)
}

/**
 * パースコンテキストにもアクセスできる関数を使用してこのパーサーの結果を変換します。
 *
 * 変換がマッチしたテキストや位置情報などのパースコンテキストからの追加情報を
 * 必要とする場合に便利です。
 *
 * @param I このパーサーが生成する入力型。
 * @param O 変換が生成する出力型。
 * @param function パースコンテキストと結果を受け取る変換関数。
 * @return 変換された値を生成するパーサー。
 */
infix fun <I : Any, O : Any> Parser<I>.mapEx(function: (ParseContext, ParseResult<I>) -> O) = Parser { context, start ->
    val result = context.parseOrNull(this, start) ?: return@Parser null
    ParseResult(function(context, result), result.start, result.end)
}

/**
 * 値だけでなく[ParseResult]自体を生成するパーサーを作成します。
 *
 * パースされた値に加えて位置情報（start/end）にアクセスする必要がある場合に便利です。
 */
val <T : Any> Parser<T>.result get() = this.mapEx { _, result -> result }
