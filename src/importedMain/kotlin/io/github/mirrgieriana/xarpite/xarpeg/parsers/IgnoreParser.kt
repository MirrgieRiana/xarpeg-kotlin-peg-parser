package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0

operator fun Parser<*>.unaryMinus() = this map { Tuple0 }
