package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.named
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for examples from the template strings tutorial (docs/en/06-template-strings.md and docs/ja/06-template-strings.md)
 */
class TemplateStringTutorialTest {

    // Define the result types
    sealed class TemplateElement
    data class StringPart(val text: String) : TemplateElement()
    data class ExpressionPart(val value: Int) : TemplateElement()

    val templateStringParser: Parser<String> = object {
        // Expression parser (reusing from earlier tutorials)
        val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
        val grouped: Parser<Int> = -'(' * ref { sum } * -')'
        val factor: Parser<Int> = number + grouped
        val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
        val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
        val expression = sum

        // String parts: match everything except $( and closing "
        // The key insight: use a regex that stops before template markers
        val stringPart: Parser<TemplateElement> =
            +Regex("""[^"$]+|\$(?!\()""") map { match ->
                StringPart(match.value)
            } named "string_part"

        // Expression part: $(...)
        val expressionPart: Parser<TemplateElement> =
            -Regex("""\$\(""") * expression * -')' map { value ->
                ExpressionPart(value)
            }

        // Template elements can be string parts or expression parts
        val templateElement = expressionPart + stringPart

        // A complete template string: "..." with any number of elements
        val templateString: Parser<String> =
            -'"' * templateElement.zeroOrMore * -'"' map { elements ->
                elements.joinToString("") { element ->
                    when (element) {
                        is StringPart -> element.text
                        is ExpressionPart -> element.value.toString()
                    }
                }
            }

        val root = templateString
    }.root

    @Test
    fun simpleString() {
        assertEquals("hello", templateStringParser.parseAll(""""hello"""").getOrThrow())
    }

    @Test
    fun stringWithOneExpression() {
        assertEquals("result: 3", templateStringParser.parseAll(""""result: $(1+2)"""").getOrThrow())
    }

    @Test
    fun expressionAtStart() {
        assertEquals("14 = answer", templateStringParser.parseAll(""""$(2*(3+4)) = answer"""").getOrThrow())
    }

    @Test
    fun multipleExpressions() {
        assertEquals("a1b2c3d", templateStringParser.parseAll(""""a$(1)b$(2)c$(3)d"""").getOrThrow())
    }

    @Test
    fun emptyString() {
        assertEquals("", templateStringParser.parseAll("""""""").getOrThrow())
    }

    @Test
    fun onlyExpression() {
        assertEquals("42", templateStringParser.parseAll(""""$(42)"""").getOrThrow())
    }

    @Test
    fun complexExpression() {
        assertEquals("result is 19", templateStringParser.parseAll(""""result is $(2*(3+4)+5)"""").getOrThrow())
    }
}
