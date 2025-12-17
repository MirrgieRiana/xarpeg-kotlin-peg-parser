package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1

val <T : Any> Parser<T>.capture: Parser<Tuple1<T>> get() = +this
