---
layout: default
title: Step 1 – Quickstart
---

# Step 1: Quickstart

Build your first parser in minutes. This guide walks through a minimal example that demonstrates the core DSL concepts.

## Your First Parser

Let's build a simple key-value parser that matches patterns like `count=42`:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    val result = kv.parseAllOrThrow("count=42")
    println(result)  // (count, 42)
    check(result == ("count" to 42))
}
```

## Understanding the Code

**Creating parsers from patterns:**
```kotlin
+Regex("[a-zA-Z][a-zA-Z0-9_]*")  // Matches identifiers starting with a letter
+Regex("[0-9]+")                  // Matches one or more digits
```
The unary `+` operator converts literals and regex patterns into parsers.

**Sequencing with `*`:**
```kotlin
identifier * -'=' * number  // Match identifier, then '=', then number
```
The `*` operator chains parsers in order. Results are packaged into typed tuples.

**Ignoring tokens with `-`:**
```kotlin
-'='  // Match the '=' character but drop it from the result
```
The unary `-` operator matches a parser but excludes its value from the result tuple.

**Transforming results with `map`:**
```kotlin
map { it.value }              // Extract string from MatchResult
map { it.value.toInt() }      // Convert string to integer
map { (key, value) -> ... }   // Destructure tuple and transform
```
The `map` function transforms parsed values into the types you need.

**Naming parsers:**
```kotlin
named "identifier"  // Assign a name for error messages
```
Named parsers appear in error messages, making debugging easier.

## Running the Parser

**Success case:**
```kotlin
kv.parseAllOrThrow("count=42")    // ✓ Returns ("count", 42)
kv.parseAllOrThrow("x=100")       // ✓ Returns ("x", 100)
```

**Error cases:**
```kotlin
kv.parseAllOrThrow("=42")         // ✗ UnmatchedInputParseException
kv.parseAllOrThrow("count")       // ✗ UnmatchedInputParseException
kv.parseAllOrThrow("count=42x")   // ✗ ExtraCharactersParseException
```

`parseAllOrThrow` requires the entire input to be consumed. It throws:
- `UnmatchedInputParseException` when no parser matches
- `ExtraCharactersParseException` when trailing input remains

## Key Takeaways

- **Unary `+`** creates parsers from literals, characters, or regex
- **Binary `*`** sequences parsers and produces tuples
- **Unary `-`** matches but drops values from results
- **`map`** transforms parsed values to your domain types
- **`named`** improves error messages
- **`parseAllOrThrow`** parses complete input or throws exceptions

## Next Steps

Now that you understand the basics, learn how to combine parsers in more sophisticated ways.

→ **[Step 2: Combinators](02-combinators.html)**
