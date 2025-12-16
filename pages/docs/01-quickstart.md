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
    check(result == ("count" to 42))  // Verifies result is (count, 42)
}
```

## Understanding the Code

**The unary `+` operator** converts literals and regex patterns into parsers:
- `+Regex("[a-zA-Z][a-zA-Z0-9_]*")` matches identifiers starting with a letter
- `+Regex("[0-9]+")` matches one or more digits

**The `*` operator** chains parsers in order (example: `identifier * -'=' * number`). Results are packaged into typed tuples.

**The unary `-` operator** matches a parser but excludes its value from the result tuple (example: `-'='` drops the `=` character).

**The `map` function** transforms parsed values:
- `map { it.value }` extracts string from MatchResult
- `map { it.value.toInt() }` converts string to integer
- `map { (key, value) -> ... }` destructures tuple and transforms

**The `named` function** assigns names to parsers for better error messages (example: `named "identifier"`).

## Running the Parser

`parseAllOrThrow` requires the entire input to be consumed:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    // Success cases
    check(kv.parseAllOrThrow("count=42") == ("count" to 42))  // ✓
    check(kv.parseAllOrThrow("x=100") == ("x" to 100))        // ✓
    
    // Error cases would throw exceptions:
    // kv.parseAllOrThrow("=42")        // ✗ UnmatchedInputParseException
    // kv.parseAllOrThrow("count")      // ✗ UnmatchedInputParseException
    // kv.parseAllOrThrow("count=42x")  // ✗ ExtraCharactersParseException
}
```

**Exception types:**
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

---

<sub>⚠️ This documentation was written by AI and may contain inaccuracies. Please verify against the source code.</sub>
