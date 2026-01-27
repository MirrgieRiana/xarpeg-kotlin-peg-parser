package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * このパーサーをマッチするが、結果を破棄するパーサーを作成します。
 *
 * このパーサーがマッチすればパーサーは成功しますが、元の値の代わりに[Tuple0]を生成します。
 */
val Parser<*>.ignore: Parser<Tuple0> get() = this map { Tuple0 }

/**
 * 単項`-`演算子を使用して結果を無視するパーサーを作成します。
 *
 * 例: `-"hello"`は"hello"をマッチしますが、結果タプルにキャプチャしません。
 */
operator fun Parser<*>.unaryMinus(): Parser<Tuple0> = this.ignore
