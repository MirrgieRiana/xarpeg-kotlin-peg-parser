# Step 6: Template strings without tokenization

One of the key advantages of PEG-style parsers is that they work directly on the input string without requiring a separate tokenization phase. This makes it straightforward to handle template strings with embedded expressions—a pattern that can be challenging for traditional lexer-based parsers.

## The challenge with tokenization

Traditional parsers with separate lexer/tokenizer phases struggle with template strings like `"hello $(1+2) world"` because:

- The lexer must decide upfront whether `$` is part of a string literal or an expression delimiter
- Nested structures (expressions inside strings inside expressions) require complex lookahead logic
- Token boundaries become ambiguous when contexts switch mid-stream

With PEG parsers that work character-by-character, you can define rules that naturally handle context switches without designing complicated token rules.

## A complete template string parser

Here's a full example that parses template strings with embedded arithmetic expressions:

```kotlin
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

// Define the result types
sealed class TemplateElement
data class StringPart(val text: String) : TemplateElement()
data class ExpressionPart(val value: Int) : TemplateElement()

val templateStringParser: Parser<String> = object {
    // Expression parser (reusing from earlier tutorials)
    val number = +Regex("[0-9]+") map { it.value.toInt() }
    val grouped: Parser<Int> by lazy { (-'(' * parser { sum } * -')') map { (_, value, _) -> value } }
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
        -Regex("""\$\(""") * expression * -')' map { (_, value, _) ->
            ExpressionPart(value)
        }

    // Template elements can be string parts or expression parts
    val templateElement = expressionPart + stringPart

    // A complete template string: "..." with any number of elements
    val templateString: Parser<String> =
        -'"' * templateElement.zeroOrMore * -'"' map { (_, elements, _) ->
            elements.joinToString("") { element ->
                when (element) {
                    is StringPart -> element.text
                    is ExpressionPart -> element.value.toString()
                }
            }
        }
    
    val root = templateString
}.root

fun main() {
    check(templateStringParser.parseAllOrThrow(""""hello"""") == "hello")
    
    check(templateStringParser.parseAllOrThrow(""""result: $(1+2)"""") == "result: 3")
    
    check(templateStringParser.parseAllOrThrow(""""$(2*(3+4)) = answer"""") == "14 = answer")
    
    check(templateStringParser.parseAllOrThrow(""""a$(1)b$(2)c$(3)d"""") == "a1b2c3d")
}
```

The Kotlin string literals above double each quote mark that should appear in the parsed input. For example, `""""hello""""` represents the input `"hello"` because each inner `"` must be escaped inside the Kotlin source string.

## How it works

The key to this parser is the `stringPart` regex:

```kotlin
+Regex("""[^"$]+|\$(?!\()""")
```

This regex pattern matches:
- `[^"$]+` — one or more characters that are neither `"` nor `$`
- `\$(?!\()` — a `$` that is **not** followed by `(` (using negative lookahead)

This regex naturally stops at template boundaries (`$(`) without needing explicit tokenization rules. When the parser encounters `$(`, it switches to `expressionPart`, which recursively invokes the expression parser.

## Nested template strings

You can extend this pattern to handle nested template strings (strings inside expressions):

```kotlin
val templateString: Parser<String> by lazy {
    -'"' * templateElement.zeroOrMore * -'"' map { (_, elements, _) ->
        elements.joinToString("") { element ->
            when (element) {
                is StringPart -> element.text
                is ExpressionPart -> element.value.toString()
            }
        }
    }
}

// Now expressions can contain template strings
val factor: Parser<Int> = number + grouped + 
    (templateString map { it.length })  // Example: use string length as a value
```

This demonstrates the power of PEG parsers: the expression parser can recursively call the template string parser, and vice versa, without pre-tokenization complexity.

## Key benefits recap

Using a PEG parser without tokenization for template strings provides:

1. **Natural context switching** — The parser adapts its rules based on what it has seen, not predetermined token boundaries
2. **Simpler grammar** — No need to design complex token rules that handle all possible contexts
3. **Recursive embedding** — Expressions can contain strings, strings can contain expressions, without special cases
4. **Regex-based boundaries** — Use negative lookahead and character classes to define natural stopping points

This approach scales well to more complex scenarios like:
- Multiple expression delimiters (`$(...)`, `#{...}`, etc.)
- Escape sequences (`\$(` to include literal `$(`)
- Different string quote styles (`"..."`, `'...'`, `"""..."""`)

Each addition is a localized change to the relevant parser, not a redesign of the entire token vocabulary.

---

Return to the guide hub for the complete tutorial path.  
← [docs/index.md](index.md)
