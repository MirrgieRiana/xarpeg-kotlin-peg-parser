---
layout: docs-en
title: Step 4 – Runtime Behavior
---

# Step 4: Runtime Behavior

Understand how parsers handle errors, consume input, and control caching for optimal performance.

## Parsing Methods

### `parseAllOrThrow`

Requires the entire input to be consumed:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"

fun main() {
    number.parseAllOrThrow("123")      // ✓ Returns 123
    // number.parseAllOrThrow("123abc") // ✗ ParseException
    // number.parseAllOrThrow("abc")    // ✗ ParseException
}
```

### Exception Types

- **`ParseException`** - Thrown when no parser matches at the current position or when parsing succeeds but trailing input remains

This exception provides a `context` property for detailed error information.

## Error Context

`ParseContext` tracks parsing failures to help build user-friendly error messages:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val letter = +Regex("[a-z]") map { it.value } named "letter"
val digit = +Regex("[0-9]") map { it.value } named "digit"
val identifier = letter * (letter + digit).zeroOrMore

fun main() {
    val result = identifier.parseAll("1abc")
    val exception = result.exceptionOrNull() as? ParseException
    
    check(exception != null)  // Parsing fails
    check(exception.context.errorPosition == 0)  // Failed at position 0
    
    val expected = exception.context.suggestedParsers
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
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val operator = (+'*' + +'+') named "operator"
val expr = number * operator * number

fun main() {
    val result = expr.parseAll("42 + 10")
    val exception = result.exceptionOrNull() as? ParseException
    
    check(exception != null)  // Parsing fails
    check(exception.context.errorPosition > 0)  // Error position tracked
    val suggestions = exception.context.suggestedParsers.mapNotNull { it.name }
    check(suggestions.isNotEmpty())  // Has suggestions
}
```

### Rich Error Messages

Use the `formatMessage` extension function to generate user-friendly error messages that include error position, expected elements, the source line, and a caret indicator pointing to the error location:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val operator = +'+' + +'-'
val expr = number * operator * number

fun main() {
    val input = "42*10"
    try {
        expr.parseAllOrThrow(input)
    } catch (exception: ParseException) {
        val message = exception.formatMessage()
        val lines = message.lines()
        check(lines[0] == "Syntax Error at 1:3")
        check(lines[1] == "Expect: \"+\", \"-\"")
        check(lines[2] == "Actual: \"*\"")
        check(lines[3] == "42*10")
        check(lines[4] == "  ^")
    }
}
```

The `formatMessage` function provides:
- Error line and column number
- List of expected named parsers (if available)
- The actual character found (or EOF)
- The source line where the error occurred
- A caret (`^`) symbol indicating the error position

## Memoization and Caching

### Default Behavior

`ParseContext` uses memoization by default to make backtracking predictable:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    // Memoization enabled (default)
    parser.parseAllOrThrow("hello", useMemoization = true)
}
```

Each `(parser, position)` pair is memoized, so repeated attempts at the same position return memoized results.

### Disabling Memoization

Disable memoization for lower memory usage when your grammar doesn't backtrack heavily:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    parser.parseAllOrThrow("hello", useMemoization = false)
}
```

**Trade-offs:**
- **Memoization enabled** - Higher memory, predictable performance with heavy backtracking
- **Memoization disabled** - Lower memory, potential performance issues with alternatives

## Error Propagation

If a `map` function throws an exception, it bubbles up and aborts parsing:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val divisionByZero = +Regex("[0-9]+") map { value ->
    val n = value.value.toInt()
    if (n == 0) error("Cannot divide by zero")
    100 / n
} named "number"

fun main() {
    divisionByZero.parseAllOrThrow("10")  // ✓ Returns 10
    // divisionByZero.parseAllOrThrow("0")  // ✗ IllegalStateException
}
```

Validate before mapping or catch and wrap errors when recovery is needed.

## Debugging Tips

### Inspect Error Details from Result

Access error context from parse result:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") named "word"

fun main() {
    val result = parser.parseAll("123")
    val exception = result.exceptionOrNull() as? ParseException
    
    check(exception != null)  // Parsing fails
    check(exception.context.errorPosition == 0)  // Error at position 0
    check(exception.context.suggestedParsers.any { it.name == "word" })  // Suggests "word"
}
```

### Check Rewind Behavior

Confirm how `optional` and `zeroOrMore` rewind on failure:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = (+Regex("[a-z]+") named "letters").optional * +Regex("[0-9]+") named "digits"

fun main() {
    // optional fails but rewinds, allowing number parser to succeed
    val result = parser.parseAllOrThrow("123")
    check(result != null)  // Succeeds
}
```

### Use Tests as Reference

Check the test suite for observed behavior:
- **[ErrorContextTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpeg/ErrorContextTest.kt)** - Error tracking examples
- **[ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpeg/ParserTest.kt)** - Comprehensive behavior tests

## Extending ParseContext

`ParseContext` is declared as `open class`, allowing you to extend it with custom state for specialized parsing needs.

### Example: Indent-Based Language Support

You can extend `ParseContext` to track indentation levels for Python-style languages:

```kotlin
import io.github.mirrgieriana.xarpeg.ParseContext

class OnlineParserParseContext(
    src: String,
    useMemoization: Boolean = true,
) : ParseContext(src, useMemoization) {
    private val indentStack = mutableListOf(0)
    
    val currentIndent: Int get() = indentStack.last()
    
    fun pushIndent(indent: Int) {
        require(indent > currentIndent)
        indentStack.add(indent)
    }
    
    fun popIndent() {
        require(indentStack.size > 1)
        indentStack.removeLast()
    }
}
```

You can then use this custom context in your parsers to validate indentation:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun indent(): Parser<String> = Parser { context, start ->
    if (context !is OnlineParserParseContext) error("Requires OnlineParserParseContext")
    val expectedIndent = context.currentIndent
    // Parse and validate indentation...
}
```

See the [online-parser sample's OnlineParser.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpeg/samples/online/parser/OnlineParser.kt) and [OnlineParserParseContext.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpeg/samples/online/parser/OnlineParserParseContext.kt) for a complete implementation.

## Key Takeaways

- **`parseAllOrThrow`** requires full consumption, throws on failure
- **Error context** provides `errorPosition` and `suggestedParsers`
- **Named parsers** appear in error messages with their assigned names
- **Memoization** is enabled by default; disable with `useMemoization = false`
- **Exceptions in `map`** bubble up and abort parsing
- **`parseOrNull`** with `ParseContext` enables detailed debugging
- **`ParseContext` is extensible** for custom parsing requirements

## Next Steps

Learn how to extract position information for error reporting and source mapping.

→ **[Step 5: Parsing Positions](05-positions.html)**
