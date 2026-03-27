---
layout: docs-en
title: Step 7 – Identifier-Delimited Strings
---

# Step 7: Identifier-Delimited Strings

Learn how to parse strings where a parsed identifier determines the closing delimiter.

## What Are Identifier-Delimited Strings?

Many languages use identifiers as delimiters for string literals:

- **XML elements** - `<tag>content</tag>`: the opening tag name determines the closing tag
- **Here-documents** - `<<EOF ... EOF`: an identifier specified at the start determines the terminator

The common characteristic is that **a value parsed at runtime determines how subsequent input is parsed**. Unlike fixed delimiters (`"` or `'`), the delimiter itself is dynamically determined as part of the input.

## XML Element Parser

Here's a parser for XML-style elements where opening and closing tags must match:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

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

fun main() {
    check(xmlElementParser.parseAll("<hello>world</hello>").getOrThrow() == XmlElement("hello", "world"))
    check(xmlElementParser.parseAll("<div>content here</div>").getOrThrow() == XmlElement("div", "content here"))
    check(xmlElementParser.parseAll("<x></x>").getOrThrow() == XmlElement("x", ""))
}
```

## How It Works

### The Key: Dynamic Parser Construction with `Parser { }`

Normal combinators (`*`, `+`, `map`, etc.) compose static parsers. However, identifier-delimited strings require **dynamically** determining the closing delimiter based on what was parsed.

The `Parser { context, start -> ... }` constructor lets you write custom parsing logic. Inside this lambda, you can:

1. Call existing parsers via `context.parseOrNull(parser, position)`
2. Dynamically construct new parsers based on parsed results
3. Scan the input character by character to find the closing delimiter

In the XML example, `context.parseOrNull(openTag, start)` parses the opening tag using a pre-defined static parser. The result `name` is then used to construct `+"</${name}>"`, a new parser that matches the specific closing tag.

### Combining Static and Dynamic Parsers

Inside the `run { }` block, static parsers (like `openTag`) are defined upfront, while dynamic parsers (like `closingTag`) are constructed inside `Parser { }`. Static parsers benefit from memoization, while dynamic parsers are generated on each invocation.

### Content Scanning

To find the closing delimiter, `pos` is advanced one character at a time while trying `context.parseOrNull(closingTag, pos)`. When the closing delimiter matches, the content between the opening tag and the closing delimiter is extracted.

## Here-Document Parser

The same technique parses here-documents:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

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

fun main() {
    check(heredocParser.parseAll("<<EOF\nhello world\nEOF").getOrThrow() == Heredoc("EOF", "hello world"))
    check(heredocParser.parseAll("<<END\nline 1\nline 2\nEND").getOrThrow() == Heredoc("END", "line 1\nline 2"))
    check(heredocParser.parseAll("<<X\n\nX").getOrThrow() == Heredoc("X", ""))
}
```

A here-document starts with `<<IDENTIFIER` followed by a newline, then the body. It terminates when the same identifier appears at the start of a line.

### Differences from the XML Element Parser

The here-document parser uses `context.src.startsWith(endMarker, pos)` directly to search for the closing delimiter. This is more efficient when the delimiter pattern is simple (newline + identifier) compared to constructing a parser object each time.

Which approach to choose depends on the situation:

- **Constructing a parser** (XML example) - useful when the closing delimiter is complex
- **Direct string operations** (here-document example) - efficient when the closing delimiter is simple

## Benefits Recap

**Dynamic grammars:**
The parser determines the grammar at parse time, enabling flexible context-dependent parsing.

**Integration with combinators:**
Existing combinators can be freely used inside the `Parser { }` constructor, seamlessly combining static and dynamic parsers.

**Parametric parsing:**
By passing parsed values as parameters to subsequent parsing, adaptive parsing based on input is achieved.

## Key Takeaways

- **`Parser { context, start -> }` constructor** - Write custom parsing logic directly
- **Dynamic parser construction** - Build closing delimiter parsers from parsed values
- **`context.parseOrNull(parser, pos)`** - Call other parsers from inside custom logic
- **Scanning pattern** - Advance character by character to find closing delimiters
- **Static vs. dynamic** - Use combinators for fixed parts, `Parser { }` for dynamic parts

## Congratulations!

You've completed the Xarpeg tutorial! You now know how to:
- Build parsers with the operator-based DSL
- Combine parsers with sequences, choices, and repetition
- Handle recursion and operator precedence
- Work with errors, caching, and debugging
- Extract position information
- Parse complex nested structures
- Handle dynamic delimiters based on identifiers

### Next Steps

- **[Explore Examples](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples)** - Study complete applications
- **[Read Tests](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonTest/kotlin)** - See all features in action
- **[Browse Source](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonMain/kotlin/io/github/mirrgieriana/xarpeg)** - Understand implementation details
- **[Build Something](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - Create your own parser!

→ **[Back to Tutorial Index](index.html)**
