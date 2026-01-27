package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

val Parser<*>.ignore: Parser<Tuple0> get() = this map { Tuple0 }
operator fun Parser<*>.unaryMinus(): Parser<Tuple0> = this.ignore
