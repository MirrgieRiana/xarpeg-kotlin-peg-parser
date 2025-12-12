package mirrg.xarpite.parser.utilities

import mirrg.xarpite.parser.ParseException
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parsers.*

/**
 * Exception thrown when the special '!' operator is evaluated in a lazy arithmetic expression.
 * This exception captures the exact position in the source where the '!' appeared.
 */
class PositionMarkerException(message: String, position: Int) : ParseException(message, position)

/**
 * Lazy arithmetic calculator grammar with position tracking.
 * 
 * This parser creates a grammar for integer arithmetic expressions that are evaluated lazily.
 * All parsers return `Parser<() -> Int>`, which are lambdas that compute the result when invoked.
 * 
 * The grammar includes:
 * - Integer literals: 0-9+
 * - Operators: +, -, *, /
 * - Parentheses for grouping: ( )
 * - Special position marker: ! (throws PositionMarkerException with the exact position when evaluated)
 * 
 * The '!' operator is useful for testing position tracking, as it allows verifying that
 * parse positions are correctly captured and can be accessed during evaluation.
 */
object LazyArithmetic {
    
    /**
     * Parser for integer literals, returns a lazy function that produces the parsed integer.
     */
    private val number: Parser<() -> Int> = 
        +Regex("[0-9]+") mapEx { _, result ->
            // result.value is a MatchResult, result.value.value is the matched string
            val value = result.value.value.toInt()
            return@mapEx { value }
        }
    
    /**
     * Parser for the special '!' position marker.
     * When evaluated, it throws a PositionMarkerException with the parse position.
     */
    private val positionMarker: Parser<() -> Int> =
        +'!' mapEx { _, result ->
            val position = result.start
            return@mapEx { throw PositionMarkerException("Position marker at index $position", position) }
        }
    
    /**
     * Primary expression: number, position marker, or parenthesized expression.
     */
    private val primary: Parser<() -> Int> by lazy {
        number + positionMarker + (-'(' * parser { expr } * -')')
    }
    
    /**
     * Multiplication and division (higher precedence).
     */
    private val term: Parser<() -> Int> by lazy {
        leftAssociative(primary, +'*' + +'/') { a, op, b -> 
            when (op) {
                '*' -> ({ a() * b() })
                '/' -> ({ a() / b() })
                else -> error("Unknown operator: $op")
            }
        }
    }
    
    /**
     * Addition and subtraction (lower precedence).
     */
    val expr: Parser<() -> Int> by lazy {
        leftAssociative(term, +'+' + +'-') { a, op, b -> 
            when (op) {
                '+' -> ({ a() + b() })
                '-' -> ({ a() - b() })
                else -> error("Unknown operator: $op")
            }
        }
    }
}
