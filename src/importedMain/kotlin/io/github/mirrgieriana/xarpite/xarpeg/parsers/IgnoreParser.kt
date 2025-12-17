package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0

val Parser<*>.ignore: Parser<Tuple0> get() = this map { Tuple0 }
operator fun Parser<*>.unaryMinus(): Parser<Tuple0> = this.ignore
