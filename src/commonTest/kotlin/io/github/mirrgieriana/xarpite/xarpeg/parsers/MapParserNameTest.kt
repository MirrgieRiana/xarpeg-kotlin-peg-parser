package io.github.mirrgieriana.xarpite.xarpeg.parsers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MapParserNameTest {
    @Test
    fun mapPreservesCharParserName() {
        val charParser = ')'.toParser()
        assertNotNull(charParser.name)
        assertEquals("\")\"", charParser.name)

        val mappedParser = charParser.ignore
        assertNotNull(mappedParser.name, "Mapped parser should preserve the original parser's name")
        assertEquals("\")\"", mappedParser.name)
    }

    @Test
    fun unaryMinusPreservesCharParserName() {
        val ignoreParser = -')'
        assertNotNull(ignoreParser.name, "Ignore parser created with unary minus should have a name")
        assertEquals("\")\"", ignoreParser.name)
    }
}
