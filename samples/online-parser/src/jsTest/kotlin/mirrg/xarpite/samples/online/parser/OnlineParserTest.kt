package mirrg.xarpite.samples.online.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class OnlineParserTest {

    @Test
    fun parsesExpressionWithWhitespaceAroundPlus() {
        assertEquals(3.0, parseExpression("1 + 2").toDouble())
    }

    @Test
    fun parsesExpressionWithWhitespaceAroundStar() {
        assertEquals(14.0, parseExpression("2 * ( 3 + 4 )").toDouble())
    }

    @Test
    fun parsesExpressionWithLeadingWhitespace() {
        assertEquals(3.0, parseExpression(" 1+2").toDouble())
    }

    @Test
    fun parsesExpressionWithTrailingWhitespace() {
        assertEquals(3.0, parseExpression("1+2 ").toDouble())
    }

    @Test
    fun parsesExpressionWithBothLeadingAndTrailingWhitespace() {
        assertEquals(3.0, parseExpression(" 1+2 ").toDouble())
    }
}
