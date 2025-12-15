---
layout: default
title: Step 1 – Quickstart
---

# Step 1: Build your first parser

This step walks through creating the smallest parser, running it, and turning input into typed values.

## Minimal sample

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    (identifier * -'=' * number map { (key, value) -> key to value }) named "key_value_pair"

fun main() {
    check(kv.parseAllOrThrow("count=42") == ("count" to 42))
}
```

- `+literal` / `+Regex("...")` create parsers that must match at the current position.
- `identifier` demonstrates an identifier that starts with a letter and may contain letters, digits, and `_`.
- `*` builds a sequence; the results are packaged into `Tuple` types.
- `-parser` matches but drops the value, which is handy for delimiters.
- `map` reshapes the result into any type you need.

## Run and verify

1. Place the snippet in any Kotlin entry point or run it as-is.
2. `parseAllOrThrow` throws if the input is not fully consumed, so you notice bad input immediately.
3. Use IDE completion and KDoc to inspect each combinator’s types and return shapes.

Next, expand the composition patterns to build richer grammars.
