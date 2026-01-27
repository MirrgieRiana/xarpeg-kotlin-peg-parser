package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple1

/**
 * このパーサーの結果を[Tuple1]でラップします。
 *
 * これは主にパーサーの組み合わせで単一の値をキャプチャするために使用されます。
 */
val <T : Any> Parser<T>.capture: Parser<Tuple1<T>> get() = +this
