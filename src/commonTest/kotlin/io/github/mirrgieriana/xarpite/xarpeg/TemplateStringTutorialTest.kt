package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for examples from the template strings tutorial (docs/06-template-strings.md)
 */
class TemplateStringTutorialTest {

    // Define the result types
    sealed class TemplateElement
    data class StringPart(val text: String) : TemplateElement()
    data class ExpressionPart(val value: Int) : TemplateElement()

    val templateStringParser: Parser<String> = object {
        // Expression parser (reusing from earlier tutorials)
        val number = +Regex("[0-9]+") map { it.value.toInt() }
        val grouped: Parser<Int> = -'(' * ref { sum } * -')' map { value -> value }
        val factor: Parser<Int> = number + grouped
        val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
        val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
        val expression = sum

        // String parts: match everything except $( and closing "
        // The key insight: use a regex that stops before template markers
        val stringPart: Parser<TemplateElement> =
            +Regex("""[^"$]+|\$(?!\()""") map { match ->
                StringPart(match.value)
            }

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
        assertEquals("hello", templateStringParser.parseAllOrThrow(""""hello""""))
    }

    @Test
    fun stringWithOneExpression() {
        assertEquals("result: 3", templateStringParser.parseAllOrThrow(""""result: $(1+2)""""))
    }

    @Test
    fun expressionAtStart() {
        assertEquals("14 = answer", templateStringParser.parseAllOrThrow(""""$(2*(3+4)) = answer""""))
    }

    @Test
    fun multipleExpressions() {
        assertEquals("a1b2c3d", templateStringParser.parseAllOrThrow(""""a$(1)b$(2)c$(3)d""""))
    }

    @Test
    fun emptyString() {
        assertEquals("", templateStringParser.parseAllOrThrow(""""""""))
    }

    @Test
    fun onlyExpression() {
        assertEquals("42", templateStringParser.parseAllOrThrow(""""$(42)""""))
    }

    @Test
    fun complexExpression() {
        assertEquals("result is 19", templateStringParser.parseAllOrThrow(""""result is $(2*(3+4)+5)""""))
    }
}
