# Kotlin PEG Parser Guide

All documentation is maintained in English; please keep additions in English as well.  
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

## When you want more detail

- For observed behavior, the tests in `imported/src/commonTest/kotlin/ParserTest.kt` are a quick reference.
- For implementation details, browse `imported/src/commonMain/kotlin/mirrg/xarpite/parser`.
- For return types and signatures, rely on IDE KDoc and code completion.
