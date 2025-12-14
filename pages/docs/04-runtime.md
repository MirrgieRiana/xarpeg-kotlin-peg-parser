---
layout: default
title: Step 4 – Runtime
---

# Step 4: Errors and runtime behavior

Review how parsers handle full consumption, exceptions, and memoization cache settings.

## Consume the entire input

`parseAllOrThrow` verifies that the input is matched from start to end and throws informative exceptions when it is not:

- No parser matches at the start: `UnmatchedInputParseException`
- A prefix matches but trailing input remains: `ExtraCharactersParseException`

If a `map` throws, the exception bubbles up and aborts parsing; validate before mapping or catch and wrap errors when you need to recover.

## Cache on or off

`ParseContext` memoizes by default so heavy backtracking stays predictable.  
Disable with `parseAllOrThrow(input, useCache = false)` if you want lower memory usage or need side effects to re-run.

## Debugging tips

- Reproduce failures with small inputs and confirm how `optional` or `zeroOrMore` rewind.
- When unsure about shapes and types, lean on IDE KDoc and completion.
- For more examples, see the tests in [imported/src/commonTest/kotlin/ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/imported/src/commonTest/kotlin/ParserTest.kt).

---

Next, learn how to work with parsing positions using `mapEx` to extract location information when you need it.  
→ [Step 5: Working with parsing positions](05-positions.md)
