---
layout: docs-en
title: Step 1 – Quickstart
---

# Step 1: Quickstart

Build your first parser in minutes. This guide walks through a minimal example that demonstrates the core DSL concepts.

## Your First Parser

Let's build a simple key-value parser that matches patterns like `count=42`:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    val result = kv.parseAll("count=42").getOrThrow()
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

`parseAll(...).getOrThrow()` requires the entire input to be consumed:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    // Success cases
    check(kv.parseAll("count=42").getOrThrow() == ("count" to 42))  // ✓
    check(kv.parseAll("x=100").getOrThrow() == ("x" to 100))        // ✓
    
    // Error cases would throw exceptions:
    // kv.parseAll("=42").getOrThrow()        // ✗ ParseException
    // kv.parseAll("count").getOrThrow()      // ✗ ParseException
    // kv.parseAll("count=42x").getOrThrow()  // ✗ ParseException
}
```

**Exception types:**
- `ParseException` when no parser matches or when trailing input remains

## Key Takeaways

- **Unary `+`** creates parsers from literals, characters, or regex
- **Binary `*`** sequences parsers and produces tuples
- **Unary `-`** matches but drops values from results
- **`map`** transforms parsed values to your domain types
- **`named`** improves error messages
- **`parseAll(...).getOrThrow()`** parses complete input or throws exceptions

## Best Practices

When choosing parser types, follow these guidelines for optimal performance and clarity:

**Use Char tokens for single characters:**
- Good: `+'x'` - efficient character matching
- Bad: `+"x"` - unnecessary string overhead
- Bad: `+Regex("x")` - regex overhead for fixed character

**Use String tokens for fixed strings:**
- Good: `+"xyz"` - efficient string matching  
- Bad: `+Regex("xyz")` - regex overhead for fixed strings

**Use Regex tokens with `named` for patterns:**
- Good: `+Regex("[0-9]+") named "number"` - named regex gives clear error messages
- Bad: `+Regex("[0-9]+")` - unnamed regex gives poor error messages

**Summary:**
- Single character → use `+'x'`, not `+"x"` or `+Regex("x")`
- Fixed string → use `+"xyz"`, not `+Regex("xyz")`
- Pattern/variable content → use `+Regex("...") named "name"`

## Next Steps

Now that you understand the basics, learn how to combine parsers in more sophisticated ways.

→ **[Step 2: Combinators](02-combinators.html)**
