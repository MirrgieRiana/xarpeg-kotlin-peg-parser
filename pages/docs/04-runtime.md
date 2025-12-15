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

## Token candidates for better error messages

When parsing fails, Xarpeg tracks which parsers were attempted at the furthest position in the input. This feature helps you provide better error messages by showing users what the parser expected.

### How it works

The `ParseContext` maintains two properties:

- `errorPosition`: The furthest position reached before failure
- `suggestedParsers`: A set of parsers that failed at that position

When a parser fails, it records suggestions only at the furthest position. Earlier failures are automatically discarded as parsing progresses deeper into the input.

### Usage example

```kotlin
val number = +Regex("[0-9]+") named "number"
val identifier = +Regex("[a-zA-Z]+") named "identifier"
val keyword = +"if" named "if keyword"

val expression = number + identifier + keyword
val context = ParseContext("@invalid", useCache = true)

val result = context.parseOrNull(expression, 0)
if (result == null) {
    val position = context.errorPosition
    val expected = context.suggestedParsers.mapNotNull { it.name }.joinToString(", ")
    println("Parse failed at position $position. Expected: $expected")
    // Output: "Parse failed at position 0. Expected: number, identifier, if keyword"
}
```

### Named parsers

Use the `named` infix function to give parsers descriptive names. Named parsers prevent their internal structure from cluttering suggestions—only the named parser itself appears in `suggestedParsers`:

```kotlin
val stringLiteral = (+'\"' * +Regex("[^\"]*") * +'\"') named "string"
// When this fails, users see "string" instead of individual quote and regex parsers
```

### Best practices

- Name top-level grammar rules (statements, expressions, literals) for clearer error messages
- Keep names short and user-friendly (e.g., "number", "identifier", "string")
- Access `suggestedParsers` through the `ParseContext` when catching parse exceptions
- Use `parser.name` or `parser.nameOrString` to get human-readable descriptions

For detailed examples, see [TokenCandidateTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/TokenCandidateTest.kt).

## Debugging tips

- Reproduce failures with small inputs and confirm how `optional` or `zeroOrMore` rewind.
- When unsure about shapes and types, lean on IDE KDoc and completion.
- Use token candidates to understand why parsing failed and what the parser expected.
- For more examples, see the tests in [src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/importedTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt).

---

Next, learn how to work with parsing positions using `mapEx` to extract location information when you need it.  
→ [Step 5: Working with parsing positions](05-positions.md)
