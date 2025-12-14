package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1
import io.github.mirrgieriana.xarpite.xarpeg.parsers.toParser
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.not
// Parser extension properties

/**
 * Extension property equivalent to unaryPlus operator.
 * Captures the parser result into a Tuple1.
 * 
 * Example:
 * ```
 * val parser = +"hello"
 * val captured = parser.capture  // equivalent to +parser
 * ```
 */
val <T : Any> Parser<T>.capture: Parser<Tuple1<T>>
    get() = +this

/**
 * Extension property equivalent to unaryMinus operator.
 * Ignores the parser result and returns Tuple0.
 * 
 * Example:
 * ```
 * val parser = +"hello"
 * val ignored = parser.ignore  // equivalent to -parser
 * ```
 */
val Parser<*>.ignore: Parser<Tuple0>
    get() = -this

/**
 * Extension property equivalent to not operator.
 * Performs negative lookahead - succeeds if the parser fails.
 * 
 * Example:
 * ```
 * val parser = +"hello"
 * val notParser = parser.not  // equivalent to !parser
 * ```
 */
val Parser<*>.not: Parser<Tuple0>
    get() = !this

// String extension properties

/**
 * Extension property equivalent to unaryPlus operator for String.
 * Converts the string to a parser and captures the result into a Tuple1.
 * 
 * Example:
 * ```
 * val captured = "hello".capture  // equivalent to +"hello"
 * ```
 */
val String.capture: Parser<Tuple1<String>>
    get() = +this.toParser()

/**
 * Extension property equivalent to unaryMinus operator for String.
 * Converts the string to a parser and ignores the result.
 * 
 * Example:
 * ```
 * val ignored = "hello".ignore  // equivalent to -"hello"
 * ```
 */
val String.ignore: Parser<Tuple0>
    get() = -this

/**
 * Extension property equivalent to not operator for String.
 * Converts the string to a parser and performs negative lookahead.
 * 
 * Example:
 * ```
 * val notParser = "hello".not  // equivalent to !"hello"
 * ```
 */
val String.not: Parser<Tuple0>
    get() = !this

// Char extension properties

/**
 * Extension property equivalent to unaryPlus operator for Char.
 * Converts the char to a parser and captures the result into a Tuple1.
 * 
 * Example:
 * ```
 * val captured = 'a'.capture  // equivalent to +'a'
 * ```
 */
val Char.capture: Parser<Tuple1<Char>>
    get() = +this.toParser()

/**
 * Extension property equivalent to unaryMinus operator for Char.
 * Converts the char to a parser and ignores the result.
 * 
 * Example:
 * ```
 * val ignored = 'a'.ignore  // equivalent to -'a'
 * ```
 */
val Char.ignore: Parser<Tuple0>
    get() = -this

/**
 * Extension property equivalent to not operator for Char.
 * Converts the char to a parser and performs negative lookahead.
 * 
 * Example:
 * ```
 * val notParser = 'a'.not  // equivalent to !'a'
 * ```
 */
val Char.not: Parser<Tuple0>
    get() = !this

// Regex extension properties

/**
 * Extension property equivalent to unaryPlus operator for Regex.
 * Converts the regex to a parser and captures the result into a Tuple1.
 * 
 * Example:
 * ```
 * val captured = Regex("[0-9]+").capture  // equivalent to +Regex("[0-9]+")
 * ```
 */
val Regex.capture: Parser<Tuple1<MatchResult>>
    get() = +this.toParser()

/**
 * Extension property equivalent to unaryMinus operator for Regex.
 * Converts the regex to a parser and ignores the result.
 * 
 * Example:
 * ```
 * val ignored = Regex("[0-9]+").ignore  // equivalent to -Regex("[0-9]+")
 * ```
 */
val Regex.ignore: Parser<Tuple0>
    get() = -this

/**
 * Extension property equivalent to not operator for Regex.
 * Converts the regex to a parser and performs negative lookahead.
 * 
 * Example:
 * ```
 * val notParser = Regex("[0-9]+").not  // equivalent to !Regex("[0-9]+")
 * ```
 */
val Regex.not: Parser<Tuple0>
    get() = !this
