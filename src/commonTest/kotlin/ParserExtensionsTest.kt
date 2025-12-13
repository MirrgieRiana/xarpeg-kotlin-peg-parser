import mirrg.xarpite.parser.ParseContext
import mirrg.xarpite.parser.Tuple0
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.capture
import mirrg.xarpite.parser.parsers.ignore
import mirrg.xarpite.parser.parsers.map
import mirrg.xarpite.parser.parsers.not
import mirrg.xarpite.parser.parsers.times
import mirrg.xarpite.parser.parsers.unaryMinus
import mirrg.xarpite.parser.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParserExtensionsTest {

    @Test
    fun capturePropertyEquivalentToUnaryPlus() {
        val parser = +"hello"
        val capturedWithOperator = +parser
        val capturedWithProperty = parser.capture
        
        val context = ParseContext("hello world", useCache = true)
        
        val resultOperator = capturedWithOperator.parseOrNull(context, 0)
        val resultProperty = capturedWithProperty.parseOrNull(context, 0)
        
        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value.a, resultProperty.value.a)
        assertEquals(resultOperator.end, resultProperty.end)
    }

    @Test
    fun capturePropertyInSequence() {
        val parser = (+"hello").capture * (+" ").ignore * (+"world").capture map { (hello, world) ->
            "$hello-$world"
        }
        
        assertEquals("hello-world", parser.parseAllOrThrow("hello world"))
    }

    @Test
    fun ignorePropertyEquivalentToUnaryMinus() {
        val parser = +"hello"
        val ignoredWithOperator = -parser
        val ignoredWithProperty = parser.ignore
        
        val context = ParseContext("hello world", useCache = true)
        
        val resultOperator = ignoredWithOperator.parseOrNull(context, 0)
        val resultProperty = ignoredWithProperty.parseOrNull(context, 0)
        
        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
        assertEquals(resultOperator.end, resultProperty.end)
        assertEquals(Tuple0, resultProperty.value)
    }

    @Test
    fun ignorePropertyInSequence() {
        val parser = (+"hello").capture * (+" ").ignore * (+"world").capture map { (hello, world) ->
            "$hello+$world"
        }
        
        assertEquals("hello+world", parser.parseAllOrThrow("hello world"))
    }

    @Test
    fun notPropertyEquivalentToNotOperator() {
        val parser = +"hello"
        val notWithOperator = !parser
        val notWithProperty = parser.not
        
        val context = ParseContext("world", useCache = true)
        
        val resultOperator = notWithOperator.parseOrNull(context, 0)
        val resultProperty = notWithProperty.parseOrNull(context, 0)
        
        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
        assertEquals(resultOperator.end, resultProperty.end)
        assertEquals(Tuple0, resultProperty.value)
    }

    @Test
    fun notPropertyFailsWhenParserMatches() {
        val parser = +"hello"
        val notParser = parser.not
        
        val context = ParseContext("hello world", useCache = true)
        val result = notParser.parseOrNull(context, 0)
        
        assertNull(result)
    }

    @Test
    fun notPropertyInSequence() {
        // Negative lookahead: match "a" only if not followed by "b"
        val parser = (+"a").ignore * (+"b").not * (+"c").capture map { (c) ->
            "matched: $c"
        }
        
        assertEquals("matched: c", parser.parseAllOrThrow("ac"))
        
        val context = ParseContext("abc", useCache = true)
        assertNull(parser.parseOrNull(context, 0))
    }

    @Test
    fun allExtensionsWorkTogether() {
        // Complex parser using all three extension properties
        val parser = 
            (+"start").ignore *
            (+"value").capture *
            (+"end").not *
            (+"middle").capture map { (value, middle) ->
                "$value-$middle"
            }
        
        assertEquals("value-middle", parser.parseAllOrThrow("startvaluemiddle"))
    }

    @Test
    fun mixingOperatorsAndProperties() {
        // Mix operators and properties to ensure they work together
        val hello = +"hello"
        val space = +" "
        val world = +"world"
        
        val parser = -hello * space.ignore * +world map { it.a }
        
        assertEquals("world", parser.parseAllOrThrow("hello world"))
    }
}
