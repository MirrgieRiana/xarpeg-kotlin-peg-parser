package mirrg.xarpite.kotlinpeg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun textAndRegex() {
        val grammar = peg<String> {
            start(text("hi").map { it.text })
        }
        assertEquals("hi", grammar.parse("hi"))
        val number = peg<Int> {
            start(regex("[0-9]+").map { it.text.toInt() })
        }
        assertEquals(123, number.parse("123"))
    }

    @Test
    fun seqAndChoice() {
        val grammar = peg<String> {
            val ab = seq(text("a"), text("b")).map { parts ->
                (parts[0] as Token).text + (parts[1] as Token).text
            }
            val c = text("c").map { it.text }
            start(choice(ab, c).map { it as String })
        }
        assertEquals("ab", grammar.parse("ab"))
        assertEquals("c", grammar.parse("c"))
    }

    @Test
    fun manyAndOptional() {
        val grammar = peg<String> {
            val sign = optional(text("-")).map { token -> if (token != null) token.text else "" }
            val digits = regex("[0-9]").many().map { tokens ->
                tokens.joinToString("") { it.text }
            }
            start(seq(sign, digits).map { parts ->
                (parts[0] as String) + (parts[1] as String)
            })
        }
        assertEquals("-1234", grammar.parse("-1234"))
        assertEquals("567", grammar.parse("567"))
    }

    @Test
    fun leftAssocArithmetic() {
        val grammar = peg<Int> {
            val number = regex("[0-9]+").map { it.text.toInt() }
            val plus = text("+")
            val minus = text("-")
            val expr = rule<Int>("expr")
            expr.define(
                leftAssoc(number, choice(plus, minus)) { left, op, right ->
                    when (op.text) {
                        "+" -> left + right
                        "-" -> left - right
                        else -> error("unreachable")
                    }
                }
            )
            start(expr)
        }
        assertEquals(3, grammar.parse("1+2-0"))
        assertEquals(-2, grammar.parse("1-3"))
    }

    @Test
    fun failureDiagnostic() {
        val grammar = peg<Token> {
            start(text("a"))
        }
        val result = grammar.tryParse("b")
        assertTrue(result is ParseResult.Failure)
        result as ParseResult.Failure
        assertEquals(0, result.position)
        assertTrue(result.expected.contains("'a'"))
    }
}
