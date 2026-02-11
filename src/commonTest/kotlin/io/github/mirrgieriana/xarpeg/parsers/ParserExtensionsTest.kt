package io.github.mirrgieriana.xarpeg.parsers

import io.github.mirrgieriana.xarpeg.ParseContext
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.parseAllOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * パーサー拡張プロパティの動作をテストする
 */
class ParserExtensionsTest {

    @Test
    fun capturePropertyEquivalentToUnaryPlus() {
        val parser = +"hello"
        val capturedWithOperator = +parser
        val capturedWithProperty = parser.capture

        val context = ParseContext("hello world", useMemoization = true)

        val resultOperator = capturedWithOperator.parseOrNull(context, 0)
        val resultProperty = capturedWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value.a, resultProperty.value.a)
        assertEquals(resultOperator.end, resultProperty.end)
    }

    @Test
    fun capturePropertyInSequence() {
        val parser = (+"hello").capture * (+' ').ignore * (+"world").capture map { (hello, world) ->
            "$hello-$world"
        }

        assertEquals("hello-world", parser.parseAllOrThrow("hello world"))
    }

    @Test
    fun ignorePropertyEquivalentToUnaryMinus() {
        val parser = +"hello"
        val ignoredWithOperator = -parser
        val ignoredWithProperty = parser.ignore

        val context = ParseContext("hello world", useMemoization = true)

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
        val parser = (+"hello").capture * (+' ').ignore * (+"world").capture map { (hello, world) ->
            "$hello+$world"
        }

        assertEquals("hello+world", parser.parseAllOrThrow("hello world"))
    }

    @Test
    fun notPropertyEquivalentToNotOperator() {
        val parser = +"hello"
        val notWithOperator = !parser
        val notWithProperty = parser.not

        val context = ParseContext("world", useMemoization = true)

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

        val context = ParseContext("hello world", useMemoization = true)
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

        val context = ParseContext("abc", useMemoization = true)
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
        val space = +' '
        val world = +"world"

        val parser = -hello * space.ignore * +world map { it.a }

        assertEquals("world", parser.parseAllOrThrow("hello world"))
    }

    // String extension property tests

    @Test
    fun stringCapturePropertyEquivalentToUnaryPlus() {
        val capturedWithProperty = "hello".capture

        val context = ParseContext("hello world", useMemoization = true)

        val resultProperty = capturedWithProperty.parseOrNull(context, 0)

        assertNotNull(resultProperty)
        assertEquals("hello", resultProperty.value.a)
        assertEquals(5, resultProperty.end)
    }

    @Test
    fun stringIgnorePropertyEquivalentToUnaryMinus() {
        val ignoredWithOperator = -"hello"
        val ignoredWithProperty = "hello".ignore

        val context = ParseContext("hello world", useMemoization = true)

        val resultOperator = ignoredWithOperator.parseOrNull(context, 0)
        val resultProperty = ignoredWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
        assertEquals(resultOperator.end, resultProperty.end)
    }

    @Test
    fun stringNotPropertyEquivalentToNotOperator() {
        val notWithOperator = !"hello"
        val notWithProperty = "hello".not

        val context = ParseContext("world", useMemoization = true)

        val resultOperator = notWithOperator.parseOrNull(context, 0)
        val resultProperty = notWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
    }

    @Test
    fun stringPropertiesInSequence() {
        val parser = "hello".ignore * " ".ignore * "world".capture map { (world) ->
            world
        }

        assertEquals("world", parser.parseAllOrThrow("hello world"))
    }

    // Char extension property tests

    @Test
    fun charCapturePropertyEquivalentToUnaryPlus() {
        val capturedWithProperty = 'a'.capture

        val context = ParseContext("abc", useMemoization = true)

        val resultProperty = capturedWithProperty.parseOrNull(context, 0)

        assertNotNull(resultProperty)
        assertEquals('a', resultProperty.value.a)
        assertEquals(1, resultProperty.end)
    }

    @Test
    fun charIgnorePropertyEquivalentToUnaryMinus() {
        val ignoredWithOperator = -'a'
        val ignoredWithProperty = 'a'.ignore

        val context = ParseContext("abc", useMemoization = true)

        val resultOperator = ignoredWithOperator.parseOrNull(context, 0)
        val resultProperty = ignoredWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
        assertEquals(resultOperator.end, resultProperty.end)
    }

    @Test
    fun charNotPropertyEquivalentToNotOperator() {
        val notWithOperator = !'a'
        val notWithProperty = 'a'.not

        val context = ParseContext("bcd", useMemoization = true)

        val resultOperator = notWithOperator.parseOrNull(context, 0)
        val resultProperty = notWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
    }

    @Test
    fun charPropertiesInSequence() {
        val parser = 'a'.ignore * 'b'.ignore * 'c'.capture map { (c) ->
            c
        }

        assertEquals('c', parser.parseAllOrThrow("abc"))
    }

    // Regex extension property tests

    @Test
    fun regexCapturePropertyEquivalentToUnaryPlus() {
        val regex = Regex("[0-9]+")
        val capturedWithProperty = regex.capture

        val context = ParseContext("123abc", useMemoization = true)

        val resultProperty = capturedWithProperty.parseOrNull(context, 0)

        assertNotNull(resultProperty)
        assertEquals("123", resultProperty.value.a.value)
        assertEquals(3, resultProperty.end)
    }

    @Test
    fun regexIgnorePropertyEquivalentToUnaryMinus() {
        val regex = Regex("[0-9]+")
        val ignoredWithOperator = -regex
        val ignoredWithProperty = regex.ignore

        val context = ParseContext("123abc", useMemoization = true)

        val resultOperator = ignoredWithOperator.parseOrNull(context, 0)
        val resultProperty = ignoredWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
        assertEquals(resultOperator.end, resultProperty.end)
    }

    @Test
    fun regexNotPropertyEquivalentToNotOperator() {
        val regex = Regex("[0-9]+")
        val notWithOperator = !regex
        val notWithProperty = regex.not

        val context = ParseContext("abc", useMemoization = true)

        val resultOperator = notWithOperator.parseOrNull(context, 0)
        val resultProperty = notWithProperty.parseOrNull(context, 0)

        assertNotNull(resultOperator)
        assertNotNull(resultProperty)
        assertEquals(resultOperator.value, resultProperty.value)
    }

    @Test
    fun regexPropertiesInSequence() {
        val parser = Regex("[0-9]+").ignore * Regex("[a-z]+").capture map { (letters) ->
            letters.value
        }

        assertEquals("abc", parser.parseAllOrThrow("123abc"))
    }

    @Test
    fun mixedTypesWithProperties() {
        // Mix String, Char, Regex, and Parser properties
        val parser =
            "start".ignore *
                '['.ignore *
                Regex("[0-9]+").capture *
                ']'.ignore *
                "end".ignore map { (num) ->
                num.value.toInt()
            }

        assertEquals(42, parser.parseAllOrThrow("start[42]end"))
    }
}
