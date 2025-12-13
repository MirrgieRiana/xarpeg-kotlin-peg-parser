# Xarpeg: Kotlin PEG Parser Guide

This guide is split into themed subpages so you can learn step by step. For API signatures, rely on IDE completion and KDoc.

When adding the library to your project, replace `<latest-version>` with the version shown on [Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases).

## Reading path (hub)

- **Step 1 – Build your first parser:** minimal DSL example and how to run it  
  → [Step 1: Quickstart](01-quickstart.html)
- **Step 2 – Combine parsers:** sequences, choices, repetition, and other core patterns  
  → [Step 2: Combinators](02-combinators.html)
- **Step 3 – Handle expressions and recursion:** using `parser {}` / `by lazy` plus associativity helpers  
  → [Step 3: Expressions](03-expressions.html)
- **Step 4 – Errors and runtime behavior:** exceptions, full consumption, cache on/off  
  → [Step 4: Runtime](04-runtime.html)
- **Step 5 – Working with parsing positions:** using `mapEx` to access positions while keeping types simple  
  → [Step 5: Positions](05-positions.html)
- **Step 6 – Template strings without tokenization:** handling embedded expressions naturally with PEG parsers  
  → [Step 6: Template strings](06-template-strings.html)

## Complete example: JSON parser

Want to see a real-world parser in action? Check out the full JSON parser implementation that handles all JSON data types including strings with escape sequences, numbers, booleans, null, arrays, and nested objects with recursion.

→ [src/commonTest/kotlin/JsonParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/JsonParserTest.kt)

This example demonstrates:
- Parsing strings with escape sequences (`\"`, `\\`, `\n`, `\uXXXX`, etc.)
- Handling numbers in various formats (integers, decimals, scientific notation)
- Building recursive parsers for arrays and objects using `by lazy` and `parser {}`
- Separating list items with custom separator parsers
- Comprehensive unit tests showing the parser in action

## When you want more detail

- For observed behavior, the tests in [imported/src/commonTest/kotlin/ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/imported/src/commonTest/kotlin/ParserTest.kt) are a quick reference.
- For implementation details, browse [imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg).
- For return types and signatures, rely on IDE KDoc and code completion.
