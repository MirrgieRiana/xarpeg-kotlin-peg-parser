---
layout: default
title: Step 2 – Combinators
---

# Step 2: Combine parsers

Use the core DSL combinators to assemble multiple pieces into one parser.

## Common operators

- `parserA * parserB` — sequence; results are packed into `TupleX`.
- `parserA + parserB` — choice (tried in order); the right side is skipped if the left succeeds.
- `-parser` — match and drop the value, ideal for delimiters or keywords.
- `parser.optional` — optional; rewinds on failure so later parsers are not blocked.
- `parser.zeroOrMore` / `oneOrMore` / `list` — repetition helpers that return `List<T>`.

## Combining option and repetition

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val sign = (+'+' + +'-').optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() }
val signedInt = sign * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

val repeatedA = (+'a').oneOrMore map { it.joinToString("") }

fun main() {
    signedInt.parseAllOrThrow("-42") // => -42
    signedInt.parseAllOrThrow("99")  // => 99
    repeatedA.parseAllOrThrow("aaaa") // => "aaaa"
}
```

- `optional` always rewinds, so it will not block what comes after. Its return is `Tuple1`, so use `it.a` or destructure with `map { (value) -> ... }`.
- Repetition results can be processed immediately with `map`; here we join the characters into a string.

## Shaping sequence results

Results from `*` arrive as `TupleX`, so destructure in `map { (a, b, c) -> … }` to build the desired type.  
Drop delimiters or unneeded values with `-parser` to keep tuple arity small.

## Input boundary matchers

Use `startOfInput` and `endOfInput` to assert position at input boundaries:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

// Match only at the beginning of input
val mustStartAtBeginning = startOfInput * +"hello"

// Reject trailing characters
val noTrailingGarbage = +"hello" * endOfInput

// Match exact input with no prefix or suffix
val exactMatch = startOfInput * +"hello" * endOfInput

fun main() {
    // These succeed
    noTrailingGarbage.parseAllOrThrow("hello")
    exactMatch.parseAllOrThrow("hello")
    
    // These fail with UnmatchedInputParseException
    // val parser1 = startOfInput * +"hello"
    // parser1.parseOrNull(ParseContext("  hello", true), 2)  // not at start
    // 
    // val parser2 = +"hello" * endOfInput
    // parser2.parseOrNull(ParseContext("hello!", true), 0)   // not at end
}
```

Both parsers return `Tuple0` and consume no input, so they compose cleanly: `Tuple0 * X = X`.

> **Note**: `parseAllOrThrow` already enforces that the entire input is consumed, so `endOfInput` is mainly useful when you need this check as part of a larger grammar or when using `parseOrNull` directly.

Next, handle recursion and associativity to build expression parsers with less code.  
→ [Step 3: Handle expressions and recursion](03-expressions.md)
