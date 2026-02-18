package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/** [Tuple0] にマッピングすることにより、このパーサーの解析結果が無視されるようにします。 */
val Parser<*>.ignore: Parser<Tuple0> get() = this map { Tuple0 }

/** [Tuple0] にマッピングすることにより、このパーサーの解析結果が無視されるようにします。 */
operator fun Parser<*>.unaryMinus(): Parser<Tuple0> = this.ignore
