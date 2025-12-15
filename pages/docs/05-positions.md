---
layout: default
title: Step 5 – Parsing Positions
---

# Step 5: Parsing Positions

Extract location information from parsed results for error reporting, source mapping, and debugging.

## Parse Results and Positions

Every successful parse returns a `ParseResult<T>` containing:
- **`value: T`** - The parsed value
- **`start: Int`** - Starting position in input
- **`end: Int`** - Ending position in input

Position information is always available, but you typically work with simple types like `Parser<Int>` until you need the positions.

## Simple Transformations with `map`

The `map` combinator works with just the value, keeping types simple:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() }

fun main() {
    val result = number.parseAllOrThrow("42")
    check(result == 42)  // Just the value, no position info
}
```

## Accessing Positions with `mapEx`

Use `mapEx` when you need position information. It receives the `ParseContext` and full `ParseResult`:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*")

val identifierWithPosition = identifier mapEx { ctx, result ->
    "${result.value.value}@${result.start}-${result.end}"
}

fun main() {
    val result = identifierWithPosition.parseAllOrThrow("hello")
    check(result == "hello@0-5")  // Includes position info
}
```

**Note:** `+Regex(...)` returns `Parser<MatchResult>`, so access the string with `result.value.value`.

## Extracting Matched Text

Get the original matched substring using the `text()` extension:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val number = +Regex("[0-9]+")

val numberWithText = number mapEx { ctx, result ->
    val matched = result.text(ctx)
    val value = matched.toInt()
    "Parsed '$matched' as $value"
}

fun main() {
    val result = numberWithText.parseAllOrThrow("123")
    check(result == "Parsed '123' as 123")  // Matched text extracted
}
```

## Calculating Line and Column Numbers

Build enhanced error reporting with line/column information:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

data class Located<T>(val value: T, val line: Int, val column: Int)

fun <T : Any> Parser<T>.withLocation(): Parser<Located<T>> = this mapEx { ctx, result ->
    val text = ctx.src.substring(0, result.start)
    val line = text.count { it == '\n' } + 1
    val column = text.length - (text.lastIndexOf('\n') + 1) + 1
    Located(result.value, line, column)
}

val keyword = +Regex("[a-z]+") map { it.value }
val keywordWithLocation = keyword.withLocation()

fun main() {
    val result = keywordWithLocation.parseAllOrThrow("hello")
    check(result.value == "hello" && result.line == 1 && result.column == 1)
}
```

## Multi-line Position Tracking

Track positions across multiple lines:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

data class Token(val value: String, val line: Int, val col: Int)

fun <T : Any> Parser<T>.withPos(): Parser<Token> = this mapEx { ctx, result ->
    val prefix = ctx.src.substring(0, result.start)
    val line = prefix.count { it == '\n' } + 1
    val col = prefix.length - (prefix.lastIndexOf('\n') + 1) + 1
    Token(result.text(ctx), line, col)
}

fun main() {
    val word = +Regex("[a-z]+") map { it.value }
    val wordWithPos = word.withPos()
    
    val input = "first\nsecond\nthird"
    val context = ParseContext(input, useCache = true)
    
    // Parse first word
    val result1 = wordWithPos.parseOrNull(context, 0)
    check(result1?.value == Token("first", 1, 1))
    
    // Parse word after first newline
    val result2 = wordWithPos.parseOrNull(context, 6)
    check(result2?.value == Token("second", 2, 1))
}
```

## Practical Example: Error Messages

Combine position tracking with error context for helpful messages:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

fun main() {
    val parser = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    
    fun parseWithErrors(input: String): Result<Int> {
        return try {
            Result.success(parser.parseAllOrThrow(input))
        } catch (e: UnmatchedInputParseException) {
            val pos = e.context.errorPosition
            val prefix = input.substring(0, pos)
            val line = prefix.count { it == '\n' } + 1
            val column = prefix.length - (prefix.lastIndexOf('\n') + 1) + 1
            val expected = e.context.suggestedParsers.mapNotNull { it.name }
            
            Result.failure(Exception(
                "Syntax error at line $line, column $column. Expected: ${expected.joinToString()}"
            ))
        }
    }
    
    val result = parseWithErrors("abc")
    check(result.isFailure)  // Parsing fails as expected
}
```

## Best Practices

**Use `map` by default** - Keep types simple when positions aren't needed (example: `val simple = +Regex("[0-9]+") map { it.value.toInt() }`).

**Use `mapEx` when needed** - Extract positions only where required.

**Isolate position logic** - Create reusable helpers like `fun <T : Any> Parser<T>.withLocation(): Parser<Located<T>>` for position tracking.

**Remember: positions are always there** - You don't need to change your parser's return type throughout your grammar. Extract position information at boundaries where you need it.

## Key Takeaways

- **`ParseResult`** includes `value`, `start`, and `end`
- **`map`** transforms values, keeping types simple
- **`mapEx`** accesses context and position information
- **`.text(ctx)`** extracts the matched substring
- **Line/column calculation** requires counting newlines
- **Position helpers** keep grammar code clean

## Next Steps

Discover how PEG parsers naturally handle template strings with embedded expressions.

→ **[Step 6: Template Strings](06-template-strings.html)**
