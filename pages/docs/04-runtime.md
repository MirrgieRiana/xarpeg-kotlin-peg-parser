---
layout: default
title: Step 4 – Runtime Behavior
---

# Step 4: Runtime Behavior

Understand how parsers handle errors, consume input, and control caching for optimal performance.

## Parsing Methods

### `parseAllOrThrow`

Requires the entire input to be consumed:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"

fun main() {
    number.parseAllOrThrow("123")      // ✓ Returns 123
    // number.parseAllOrThrow("123abc") // ✗ ExtraCharactersParseException
    // number.parseAllOrThrow("abc")    // ✗ UnmatchedInputParseException
}
```

### Exception Types

- **`UnmatchedInputParseException`** - No parser matched at the current position
- **`ExtraCharactersParseException`** - Parsing succeeded but trailing input remains

Both exceptions provide a `context` property for detailed error information.

## Error Context

`ParseContext` tracks parsing failures to help build user-friendly error messages:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val letter = (+Regex("[a-z]")) named "letter" map { it.value }
val digit = (+Regex("[0-9]")) named "digit" map { it.value }
val identifier = letter * (letter + digit).zeroOrMore

fun main() {
    val context = ParseContext("1abc", useCache = true)
    val result = identifier.parseOrNull(context, 0)
    
    check(result == null)  // Parsing fails
    check(context.errorPosition == 0)  // Failed at position 0
    
    val expected = context.suggestedParsers
        .mapNotNull { it.name }
        .distinct()
        .sorted()
        .joinToString(", ")
    
    check(expected == "letter")  // Expected "letter"
}
```

### Error Tracking Properties

- **`errorPosition`** - Furthest position attempted during parsing
- **`suggestedParsers`** - Set of parsers that failed at `errorPosition`

As parsing proceeds:
1. When a parser fails further than `errorPosition`, it updates and `suggestedParsers` clears
2. Parsers failing at the current `errorPosition` are added to `suggestedParsers`
3. Named parsers appear using their assigned names

### Using Error Context with Exceptions

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val number = (+Regex("[0-9]+")) named "number" map { it.value.toInt() }
val operator = (+'*' + +'+') named "operator"
val expr = number * operator * number

fun main() {
    try {
        expr.parseAllOrThrow("42 + 10")
        error("Should have thrown exception")
    } catch (e: UnmatchedInputParseException) {
        check(e.context.errorPosition > 0)  // Error position tracked
        val suggestions = e.context.suggestedParsers.mapNotNull { it.name }
        check(suggestions.isNotEmpty())  // Has suggestions
    }
}
```

## Memoization and Caching

### Default Behavior

`ParseContext` caches results by default to make backtracking predictable:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value }

fun main() {
    // Cache enabled (default)
    parser.parseAllOrThrow("hello", useCache = true)
}
```

Each `(parser, position)` pair is memoized, so repeated attempts at the same position return cached results.

### Disabling Cache

Disable caching for lower memory usage when your grammar doesn't backtrack heavily:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value }

fun main() {
    parser.parseAllOrThrow("hello", useCache = false)
}
```

**Trade-offs:**
- **Cache enabled** - Higher memory, predictable performance with heavy backtracking
- **Cache disabled** - Lower memory, potential performance issues with alternatives

## Error Propagation

If a `map` function throws an exception, it bubbles up and aborts parsing:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val divisionByZero = +Regex("[0-9]+") map { value ->
    val n = value.value.toInt()
    if (n == 0) error("Cannot divide by zero")
    100 / n
}

fun main() {
    divisionByZero.parseAllOrThrow("10")  // ✓ Returns 10
    // divisionByZero.parseAllOrThrow("0")  // ✗ IllegalStateException
}
```

Validate before mapping or catch and wrap errors when recovery is needed.

## Debugging Tips

### Use `parseOrNull` Directly

Work with `ParseContext` directly to inspect error details:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val parser = (+Regex("[a-z]+")) named "word"

fun main() {
    val context = ParseContext("123", useCache = true)
    val result = context.parseOrNull(parser, 0)
    
    check(result == null)  // Parsing fails
    check(context.errorPosition == 0)  // Error at position 0
    check(context.suggestedParsers.any { it.name == "word" })  // Suggests "word"
}
```

### Check Rewind Behavior

Confirm how `optional` and `zeroOrMore` rewind on failure:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val parser = (+Regex("[a-z]+")).optional * +Regex("[0-9]+")

fun main() {
    val context = ParseContext("123", useCache = true)
    val result = parser.parseOrNull(context, 0)
    // optional fails but rewinds, allowing number parser to succeed
    check(result != null)  // Succeeds
}
```

### Use Tests as Reference

Check the test suite for observed behavior:
- **[ErrorContextTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ErrorContextTest.kt)** - Error tracking examples
- **[ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt)** - Comprehensive behavior tests

## Key Takeaways

- **`parseAllOrThrow`** requires full consumption, throws on failure
- **Error context** provides `errorPosition` and `suggestedParsers`
- **Named parsers** appear in error messages with their assigned names
- **Memoization** is enabled by default; disable with `useCache = false`
- **Exceptions in `map`** bubble up and abort parsing
- **`parseOrNull`** with `ParseContext` enables detailed debugging

## Next Steps

Learn how to extract position information for error reporting and source mapping.

→ **[Step 5: Parsing Positions](05-positions.html)**
