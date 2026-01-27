package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0

/**
 * Creates a parser that matches this parser but discards the result.
 *
 * The parser succeeds if this parser matches, but produces [Tuple0] instead of the original value.
 */
val Parser<*>.ignore: Parser<Tuple0> get() = this map { Tuple0 }

/**
 * Creates a parser that ignores the result using the unary `-` operator.
 *
 * Example: `-"hello"` matches "hello" but doesn't capture it in the result tuple.
 */
operator fun Parser<*>.unaryMinus(): Parser<Tuple0> = this.ignore
