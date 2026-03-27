package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for examples from the identifier-delimited strings tutorial
 * (docs/en/07-identifier-delimited-strings.md and docs/ja/07-identifier-delimited-strings.md)
 */
class IdentifierDelimitedStringTutorialTest {

    // XML element parser

    data class XmlElement(val tag: String, val content: String)

    val xmlElementParser: Parser<XmlElement> = run {
        val tagName = +Regex("[a-zA-Z][a-zA-Z0-9]*") map { it.value }
        val openTag = -'<' * tagName * -'>'

        Parser { context, start ->
            val openResult = context.parseOrNull(openTag, start) ?: return@Parser null
            val name = openResult.value

            val closingTag = +"</${name}>"

            var pos = openResult.end
            while (pos <= context.src.length) {
                val closeResult = context.parseOrNull(closingTag, pos)
                if (closeResult != null) {
                    val content = context.src.substring(openResult.end, pos)
                    return@Parser ParseResult(XmlElement(name, content), start, closeResult.end)
                }
                if (pos >= context.src.length) break
                pos++
            }
            null
        }
    }

    @Test
    fun xmlSimpleElement() {
        assertEquals(XmlElement("hello", "world"), xmlElementParser.parseAll("<hello>world</hello>").getOrThrow())
    }

    @Test
    fun xmlContentWithSpaces() {
        assertEquals(XmlElement("div", "content here"), xmlElementParser.parseAll("<div>content here</div>").getOrThrow())
    }

    @Test
    fun xmlEmptyContent() {
        assertEquals(XmlElement("x", ""), xmlElementParser.parseAll("<x></x>").getOrThrow())
    }

    @Test
    fun xmlMismatchedTagsFail() {
        assertTrue(xmlElementParser.parseAll("<a>text</b>").isFailure)
    }

    @Test
    fun xmlSingleCharTag() {
        assertEquals(XmlElement("p", "text"), xmlElementParser.parseAll("<p>text</p>").getOrThrow())
    }

    // Heredoc parser

    data class Heredoc(val delimiter: String, val content: String)

    val heredocParser: Parser<Heredoc> = run {
        val identifier = +Regex("[A-Za-z_][A-Za-z0-9_]*") map { it.value }
        val header = -"<<" * identifier * -'\n'

        Parser { context, start ->
            val headerResult = context.parseOrNull(header, start) ?: return@Parser null
            val delimiter = headerResult.value

            val endMarker = "\n${delimiter}"
            var pos = headerResult.end
            while (pos + endMarker.length <= context.src.length) {
                if (context.src.startsWith(endMarker, pos)) {
                    val content = context.src.substring(headerResult.end, pos)
                    return@Parser ParseResult(Heredoc(delimiter, content), start, pos + endMarker.length)
                }
                pos++
            }
            null
        }
    }

    @Test
    fun heredocSingleLine() {
        assertEquals(Heredoc("EOF", "hello world"), heredocParser.parseAll("<<EOF\nhello world\nEOF").getOrThrow())
    }

    @Test
    fun heredocMultiLine() {
        assertEquals(Heredoc("END", "line 1\nline 2"), heredocParser.parseAll("<<END\nline 1\nline 2\nEND").getOrThrow())
    }

    @Test
    fun heredocEmptyContent() {
        assertEquals(Heredoc("X", ""), heredocParser.parseAll("<<X\n\nX").getOrThrow())
    }

    @Test
    fun heredocDifferentDelimiters() {
        assertEquals(Heredoc("HTML", "<div>hello</div>"), heredocParser.parseAll("<<HTML\n<div>hello</div>\nHTML").getOrThrow())
    }

    @Test
    fun heredocMismatchedDelimiterFails() {
        assertTrue(heredocParser.parseAll("<<EOF\ncontent\nEND").isFailure)
    }
}
