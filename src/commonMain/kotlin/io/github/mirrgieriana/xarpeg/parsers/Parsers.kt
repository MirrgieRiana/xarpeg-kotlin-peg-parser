package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple1

val <T : Any> Parser<T>.capture: Parser<Tuple1<T>> get() = +this
