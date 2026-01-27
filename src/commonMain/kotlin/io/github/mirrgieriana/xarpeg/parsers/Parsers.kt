package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple1

/**
 * Wraps this parser's result in a [Tuple1].
 *
 * This is primarily used for capturing single values in parser combinations.
 */
val <T : Any> Parser<T>.capture: Parser<Tuple1<T>> get() = +this
