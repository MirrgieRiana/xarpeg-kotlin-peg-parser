package io.github.mirrgieriana.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpeg.ParseResult
import io.github.mirrgieriana.xarpeg.Parser
import io.github.mirrgieriana.xarpeg.Tuple0
import io.github.mirrgieriana.xarpeg.parsers.ignore
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore

/**
 * Parsers for indent-aware whitespace handling.
 * Works with [OnlineParserParseContext] to enforce indentation rules within indent blocks.
 */
internal object IndentParsers {

    /** Matches a single newline sequence: \r\n, \n, or bare \r */
    val newline = -Regex("\\r\\n|[\\r\\n]")

    /**
     * Matches the required indentation after a newline.
     * Inside an indent block ([OnlineParserParseContext.isInIndentBlock] is true),
     * requires at least [OnlineParserParseContext.currentIndent] spaces/tabs.
     * Outside an indent block, matches zero characters (always succeeds).
     */
    val indent: Parser<Tuple0> = Parser { context, pos ->
        if (context is OnlineParserParseContext && context.isInIndentBlock) {
            var spaceEnd = pos
            while (spaceEnd < context.src.length &&
                (context.src[spaceEnd] == ' ' || context.src[spaceEnd] == '\t')
            ) spaceEnd++
            if (spaceEnd - pos < context.currentIndent) return@Parser null
            ParseResult(Tuple0, pos, spaceEnd)
        } else {
            ParseResult(Tuple0, pos, pos)
        }
    }

    /** Matches a newline followed by required indentation */
    val newlineAndIndent = newline * indent

    /**
     * Matches any amount of whitespace including newlines.
     * Within indent blocks, newlines must be followed by the required indentation.
     */
    val whitespace = (-Regex("[ \t]*") * newlineAndIndent).zeroOrMore.ignore * -Regex("[ \t]*")
}
