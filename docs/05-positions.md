# Step 5: Working with parsing positions

Parsing positions implicitly accompany parse results. With `mapEx`, you can access position information while keeping your `Parser<T>` types simple.

## Understanding parse results and positions

Every successful parse returns a `ParseResult<T>` that contains:
- `value: T` — the parsed value
- `start: Int` — the starting position in the input
- `end: Int` — the ending position in the input

While building parsers, you typically work with simple types like `Parser<Int>` or `Parser<String>`, but position information is always available when you need it.

## Using `map` for simple transformations

The `map` combinator keeps your types simple by passing only the parsed value:

```kotlin
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() }

number.parseAllOrThrow("42") // => 42 (just the value)
```

This is ideal when you don't need position information and want to keep your code clean.

## Using `mapEx` to access positions

When you need position information, use `mapEx`. It receives both the `ParseContext` and the full `ParseResult`:

```kotlin
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.text
import mirrg.xarpite.parser.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*")

// Access position information without changing the parser's type
val identifierWithPosition = identifier mapEx { ctx, result ->
    "${result.value.value}@${result.start}-${result.end}"
}

identifierWithPosition.parseAllOrThrow("hello") // => "hello@0-5"
```

Notice that even though we access position information, the result type is still simple: `Parser<String>`.

**Note:** `+Regex(...)` returns a `Parser<MatchResult>`, so you access the matched string with `result.value.value` or use `map { it.value }` first for cleaner code.

## Practical example: Enhanced error messages

Position information is particularly useful for generating helpful error messages:

```kotlin
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

data class Located<T>(val value: T, val line: Int, val column: Int)

fun <T : Any> Parser<T>.withLocation(): Parser<Located<T>> = this mapEx { ctx, result ->
    // Calculate line and column from position
    val text = ctx.src.substring(0, result.start)
    val line = text.count { it == '\n' } + 1
    val column = text.length - (text.lastIndexOf('\n') + 1) + 1
    Located(result.value, line, column)
}

val keyword = +Regex("[a-z]+") map { it.value }
val keywordWithLocation = keyword.withLocation()

val result = keywordWithLocation.parseAllOrThrow("hello")
// => Located(value=hello, line=1, column=1)
```

## Getting the matched text

You can also extract the original matched text using the `text()` extension:

```kotlin
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.text
import mirrg.xarpite.parser.parsers.*

val number = +Regex("[0-9]+")

val numberWithText = number mapEx { ctx, result ->
    val matched = result.text(ctx)
    val value = matched.toInt()
    "Parsed '$matched' as $value"
}

numberWithText.parseAllOrThrow("123") // => "Parsed '123' as 123"
```

## Best practices

- **Use `map` by default** to keep types simple and code readable
- **Use `mapEx` when needed** for positions, matched text, or context-aware transformations
- **Keep position logic isolated** in helper functions like `withLocation()` to avoid cluttering your grammar
- **Remember: positions are always there** — you don't need to change your parser's type to access them

The key insight is that parsing positions implicitly accompany every parse result. You can work with simple types like `Parser<Int>` or `Parser<String>` throughout your grammar, and only extract position information at the boundaries where you need it using `mapEx`.

---

Return to the guide hub for more patterns and advanced features.  
← [docs/index.md](index.md)
