# Step 4: Errors and runtime behavior

Finish by checking how parsers handle full consumption, exceptions, and memoization cache settings.

## Consume the entire input

`parseAllOrThrow` verifies that the input is matched from start to end and throws informative exceptions when it is not:

- Nothing matches at the start: `UnmatchedInputParseException`
- A prefix matches but trailing input remains: `ExtraCharactersParseException`

If a `map` throws, that branch simply fails, which lets you embed validation inside transformations.

## Cache on or off

`ParseContext` memoizes by default so heavy backtracking stays predictable.  
Disable with `parseAllOrThrow(input, useCache = false)` if you want lower memory usage or need side effects to re-run.

## Debugging tips

- Reproduce failures with small inputs and confirm how `optional` or `zeroOrMore` rewind.
- When unsure about shapes and types, lean on IDE KDoc and completion.
- For more examples, see the tests in `imported/src/commonTest/kotlin/ParserTest.kt`.

---

Nice work! Combine these steps to build parsers tailored to your domain.
