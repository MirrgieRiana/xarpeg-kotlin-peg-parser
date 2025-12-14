---
layout: default
title: Step 4 – Runtime
---

# Step 4: Errors and runtime behavior

Review how parsers handle full consumption, exceptions, and memoization cache settings.

## Consume the entire input

`parseAllOrThrow` verifies that the input is matched from start to end and throws informative exceptions when it is not:

- No parser matched at the start: `UnmatchedInputParseException`
- A prefix matches but trailing input remains: `ExtraCharactersParseException`

If a `map` throws, the exception bubbles up and aborts parsing; validate before mapping or catch and wrap errors when you need to recover.

## Cache on or off

`ParseContext` memoizes by default so heavy backtracking stays predictable.  
Disable with `parseAllOrThrow(input, useCache = false)` if you want lower memory usage or need side effects to re-run.

## Error tracking

`ParseContext` tracks two pieces of information that can help diagnose parse failures:

- **`errorPosition`**: The furthest position in the input where parsing failed
- **`suggestedParsers`**: The set of parsers that were attempted at the `errorPosition`

These fields are automatically maintained as parsing progresses. When a parser fails:

1. If it fails at a position further than the current `errorPosition`, the `errorPosition` is updated and `suggestedParsers` is cleared
2. If it fails at the same position as `errorPosition`, the parser is added to `suggestedParsers`
3. If it fails at an earlier position, neither field is updated

This information is particularly useful for generating helpful error messages that show where parsing stopped and what was expected at that point.

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

fun main() {
    val parser = +'a' * +'b' * +'c'
    val context = ParseContext("abx", useCache = true)
    val result = context.parseOrNull(parser, 0)

    if (result == null) {
        println("Parsing failed at position ${context.errorPosition}")
        println("Expected one of: ${context.suggestedParsers}")
        // Output: Parsing failed at position 2
        // Expected one of: [CharParser('c')]
    }
}
```

These fields are updated regardless of whether caching is enabled or disabled.

## Debugging tips

- Reproduce failures with small inputs and confirm how `optional` or `zeroOrMore` rewind.
- Use `errorPosition` and `suggestedParsers` to identify where parsing stopped and what was expected.
- When unsure about shapes and types, lean on IDE KDoc and completion.
- For more examples, see the tests in [src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt).

---

Next, learn how to work with parsing positions using `mapEx` to extract location information when you need it.  
→ [Step 5: Working with parsing positions](05-positions.md)
