# Kotlin PEG Parser Guide

This guide is split into themed subpages so you can learn step by step. For API signatures, rely on IDE completion and KDoc.

## Reading path (hub)

- **Step 1 – Build your first parser:** minimal DSL example and how to run it  
  → [01-quickstart.md](01-quickstart.md)
- **Step 2 – Combine parsers:** sequences, choices, repetition, and other core patterns  
  → [02-combinators.md](02-combinators.md)
- **Step 3 – Handle expressions and recursion:** using `parser {}` / `by lazy` plus associativity helpers  
  → [03-expressions.md](03-expressions.md)
- **Step 4 – Errors and runtime behavior:** exceptions, full consumption, cache on/off  
  → [04-runtime.md](04-runtime.md)
- **Step 5 – Working with parsing positions:** using `mapEx` to access positions while keeping types simple  
  → [05-positions.md](05-positions.md)
- **Step 6 – Template strings without tokenization:** handling embedded expressions naturally with PEG parsers  
  → [06-template-strings.md](06-template-strings.md)

## Complete example: JSON parser

Want to see a real-world parser in action? Check out the full JSON parser implementation that handles all JSON data types including strings with escape sequences, numbers, booleans, null, arrays, and nested objects with recursion.

→ [src/commonTest/kotlin/JsonParserTest.kt](https://github.com/MirrgieRiana/kotlin-peg-parser/blob/main/src/commonTest/kotlin/JsonParserTest.kt)

This example demonstrates:
- Parsing strings with escape sequences (`\"`, `\\`, `\n`, `\uXXXX`, etc.)
- Handling numbers in various formats (integers, decimals, scientific notation)
- Building recursive parsers for arrays and objects using `by lazy` and `parser {}`
- Separating list items with custom separator parsers
- Comprehensive unit tests showing the parser in action

## When you want more detail

- For observed behavior, the tests in `imported/src/commonTest/kotlin/ParserTest.kt` are a quick reference.
- For implementation details, browse `imported/src/commonMain/kotlin/mirrg/xarpite/parser`.
- For return types and signatures, rely on IDE KDoc and code completion.
