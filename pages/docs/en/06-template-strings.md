---
layout: docs-en
title: Step 6 – Template Strings
---

# Step 6: Template Strings

Learn how PEG-style parsers naturally handle template strings with embedded expressions—without tokenization.

## Why No Tokenizer?

Traditional parsers with separate lexer/tokenizer phases struggle with template strings like `"hello $(1+2) world"`:

- **Ambiguous boundaries** - Is `$` part of the string or an expression delimiter?
- **Context switching** - Token rules must handle all possible contexts upfront
- **Nested structures** - Expressions inside strings inside expressions require complex lookahead

PEG parsers working character-by-character naturally handle context switches without designing complicated token rules.

## Complete Template String Parser

Here's a parser for template strings with embedded arithmetic expressions:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

sealed class TemplateElement
data class StringPart(val text: String) : TemplateElement()
data class ExpressionPart(val value: Int) : TemplateElement()

val templateStringParser: Parser<String> = object {
    // Expression parser (arithmetic with precedence)
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val grouped: Parser<Int> = -'(' * ref { sum } * -')'
    val factor: Parser<Int> = number + grouped
    val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
    val expression = sum

    // String parts: match everything except $( and closing "
    val stringPart: Parser<TemplateElement> =
        +Regex("""[^"$]+|\$(?!\()""") map { match ->
            StringPart(match.value)
        } named "string_part"

    // Expression part: $(...)
    val expressionPart: Parser<TemplateElement> =
        -+"$(" * expression * -')' map { value ->
            ExpressionPart(value)
        }

    // Template elements can be string parts or expression parts
    val templateElement = expressionPart + stringPart

    // Complete template string: "..." with any number of elements
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

fun main() {
    check(templateStringParser.parseAll(""""hello"""").getOrThrow() == "hello")
    check(templateStringParser.parseAll(""""result: $(1+2).getOrThrow()"""") == "result: 3")
    check(templateStringParser.parseAll(""""$(2*(3+4).getOrThrow()) = answer"""") == "14 = answer")
    check(templateStringParser.parseAll(""""a$(1).getOrThrow()b$(2)c$(3)d"""") == "a1b2c3d")
}
```

**Note:** In Kotlin string literals, `""""hello""""` represents the input `"hello"` because inner quotes must be escaped.

## How It Works

### The Key: Smart Regex Boundaries

The pattern `+Regex("""[^"$]+|\$(?!\()""")` matches:
- **`[^"$]+`** - One or more characters that are neither `"` nor `$`
- **`\$(?!\()` - A `$` NOT followed by `(` (negative lookahead)

The regex naturally stops at template boundaries (`$(`) without explicit tokenization. When `$(` is encountered, control passes to `expressionPart`, which recursively invokes the expression parser.

### Context Switching

The choice combinator `val templateElement = expressionPart + stringPart` handles context switching.

Try to parse an expression first. If that fails (no `$(` found), parse a string part. This naturally alternates between contexts as needed.

### Recursion

The `grouped` parser uses `ref { sum }` (example: `val grouped: Parser<Int> = -'(' * ref { sum } * -')'`) to allow parenthesized sub-expressions.

This enables nested expressions like `$(2*(3+4))`.

## Nested Template Strings

Extend the pattern to handle strings inside expressions:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

sealed class TemplateElement
data class StringPart(val text: String) : TemplateElement()
data class ExpressionPart(val value: Int) : TemplateElement()

object TemplateWithNestedStrings {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val grouped: Parser<Int> = -'(' * ref { sum } * -')'

    val stringPart: Parser<TemplateElement> =
        +Regex("""[^"$]+|\$(?!\()""") map { match -> StringPart(match.value) } named "string_part"

    val expressionPart: Parser<TemplateElement> =
        -+"$(" * ref { sum } * -')' map { value ->
            ExpressionPart(value)
        }

    val templateElement = expressionPart + stringPart

    val templateString: Parser<String> = ref {
        -'"' * templateElement.zeroOrMore * -'"' map { elements ->
            elements.joinToString("") { element ->
                when (element) {
                    is StringPart -> element.text
                    is ExpressionPart -> element.value.toString()
                }
            }
        }
    }

    // Expressions can now contain template strings
    val factor: Parser<Int> = number + grouped + (templateString map { it.length })
    val sum: Parser<Int> = leftAssociative(factor, -'+') { a, _, b -> a + b }
}

fun main() {
    TemplateWithNestedStrings.templateString.parseAll("\"nested $(1+2).getOrThrow()\"")
}
```

The expression parser can recursively call the template string parser, and vice versa. This mutual recursion works naturally with PEG—no pre-tokenization complexity.

## Benefits Recap

**Natural context switching:**
The parser adapts based on what it has seen, not predetermined token boundaries.

**Simpler grammar:**
No complex token rules that must handle all possible contexts upfront.

**Recursive embedding:**
Expressions can contain strings, strings can contain expressions—without special cases.

**Regex-based boundaries:**
Use negative lookahead and character classes to define natural stopping points.

## Extending Further

This approach scales well to more complex scenarios:

**Multiple delimiters** - Support both `$(...)` and `#{...}` by creating multiple expression part parsers and combining them.

**Escape sequences** - Match `\$(` as literal text using patterns like `+Regex("""(?:[^"$\\]|\\.)+|\$(?!\()""")`.

**Different quote styles** - Support single and double quotes by creating parsers for each style and combining with choice.

Each addition is a localized change to the relevant parser, not a redesign of the entire token vocabulary.

## Key Takeaways

- **No tokenizer** - Parse directly from characters
- **Regex boundaries** - Use negative lookahead to define stopping points
- **Choice combinator** - Natural context switching between alternatives
- **Recursion** - Mutual recursion between string and expression parsers
- **Scalable** - Easy to extend with new delimiters or escape sequences

## Congratulations!

You've completed the Xarpeg tutorial! You now know how to:
- Build parsers with the operator-based DSL
- Combine parsers with sequences, choices, and repetition
- Handle recursion and operator precedence
- Work with errors, caching, and debugging
- Extract position information
- Parse complex nested structures

### Next Steps

- **[Explore Examples](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples)** - Study complete applications
- **[Read Tests](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonTest/kotlin)** - See all features in action
- **[Browse Source](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/importedMain/kotlin/io/github/mirrgieriana/xarpeg)** - Understand implementation details
- **[Build Something](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - Create your own parser!

→ **[Back to Tutorial Index](index.html)**
