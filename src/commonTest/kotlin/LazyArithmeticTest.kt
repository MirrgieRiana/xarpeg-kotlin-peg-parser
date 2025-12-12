import mirrg.xarpite.parser.ParseException
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exception thrown when the special '!' operator is evaluated.
 */
class PositionMarkerException(message: String, position: Int) : ParseException(message, position)

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
            +'!' mapEx { _, result ->
                val position = result.start
                return@mapEx { throw PositionMarkerException("Position marker at index $position", position) }
            }
        
        private val primary: Parser<() -> Int> by lazy {
            number + positionMarker + (-'(' * parser { expr } * -')')
        }
        
        private val term: Parser<() -> Int> by lazy {
            leftAssociative(primary, +'*' + +'/') { a, op, b -> 
                when (op) {
                    '*' -> ({ a() * b() })
                    '/' -> ({ a() / b() })
                    else -> error("Unknown operator: $op")
                }
            }
        }
        
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
