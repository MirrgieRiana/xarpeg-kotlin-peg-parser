package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.ParseException
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exception thrown when the special '!' operator is evaluated.
 */
class PositionMarkerException(message: String, context: ParseContext, position: Int) : ParseException(message, context, position)

/**
 * Test demonstrating position tracking using mapEx with lazy arithmetic parser.
 * The parser evaluates integer arithmetic lazily and uses '!' to mark positions.
 */
class LazyArithmeticTest {
    
    // Lazy arithmetic parser implementation
    private object LazyArithmetic {
        private val number: Parser<() -> Int> = 
            +Regex("[0-9]+") mapEx { _, result ->
                val value = result.value.value.toInt()
                return@mapEx { value }
            }
        
        private val positionMarker: Parser<() -> Int> =
            +'!' mapEx { context, result ->
                val position = result.start
                return@mapEx { throw PositionMarkerException("Position marker at index $position", context, position) }
            }
        
        private val primary: Parser<() -> Int> =
            number + positionMarker + (-'(' * ref { expr } * -')')
        
        private val term: Parser<() -> Int> =
            leftAssociative(primary, +'*' + +'/') { a, op, b -> 
                when (op) {
                    '*' -> ({ a() * b() })
                    '/' -> ({ a() / b() })
                    else -> error("Unknown operator: $op")
                }
            }
        
        val expr: Parser<() -> Int> =
            leftAssociative(term, +'+' + +'-') { a, op, b -> 
                when (op) {
                    '+' -> ({ a() + b() })
                    '-' -> ({ a() - b() })
                    else -> error("Unknown operator: $op")
                }
            }
    }

    @Test
    fun positionMarkerAtStart() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("!")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(0, exception.position)
    }

    @Test
    fun positionMarkerAfterNumber() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("42+!")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(3, exception.position)
    }

    @Test
    fun positionMarkerInMiddle() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("1+!+3")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(2, exception.position)
    }

    @Test
    fun positionMarkerInsideParentheses() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("(2+!)")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(3, exception.position)
    }

    @Test
    fun positionMarkerWithComplexExpression() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("(10+20)*!")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(8, exception.position)
    }

    @Test
    fun positionMarkerDeepNesting() {
        val lazyResult = LazyArithmetic.expr.parseAllOrThrow("((1+2)*(3+!))")
        val exception = assertFailsWith<PositionMarkerException> {
            lazyResult()
        }
        assertEquals(10, exception.position)
    }
}
