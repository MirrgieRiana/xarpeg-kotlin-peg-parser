---
layout: default
title: Step 2 – Combinators
---

# Step 2: Combinators

Learn to combine parsers using sequences, choices, repetition, and more to build complex grammars.

## Core Combinators

### Choice with `+`

Try alternatives in order. The first match wins:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val keyword = (+"if" + +"while" + +"for") named "keyword"

fun main() {
    keyword.parseAllOrThrow("if")      // ✓ matches "if"
    keyword.parseAllOrThrow("while")   // ✓ matches "while"
}
```

### Optional Parsing

`optional` attempts to match but rewinds on failure. Returns `Tuple1<T?>`:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val sign = ((+'+') map { '+' }) + ((+'-') map { '-' })
val signOpt = sign.optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() }
val signedInt = signOpt * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

fun main() {
    check(signedInt.parseAllOrThrow("-42") == -42)
    check(signedInt.parseAllOrThrow("99") == 99)
}
```

Use `it.a` to access the optional value, or destructure with `map { (value) -> ... }`.

### Repetition

Collect multiple matches into a list:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val digits = (+Regex("[0-9]")).oneOrMore map { matches -> 
    matches.joinToString("") { it.value }
}

val letters = (+Regex("[a-z]")).zeroOrMore map { matches -> 
    matches.map { it.value }
}

fun main() {
    digits.parseAllOrThrow("123")    // => "123"
    letters.parseAllOrThrow("abc")   // => ["a", "b", "c"]
    letters.parseAllOrThrow("")      // => []
}
```

- **`.zeroOrMore`** - Matches zero or more times (never fails)
- **`.oneOrMore`** - Matches one or more times (fails if no match)
- **`.list(min, max)`** - Matches between `min` and `max` times

## Shaping Results

Sequences with `*` return tuples. Use `-parser` to drop unneeded values:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

// Without dropping: Tuple3<MatchResult, MatchResult, MatchResult>
val withDelimiters = +'(' * +Regex("[a-z]+") * +')'

// With dropping: MatchResult (just the middle value)
val cleanResult = -'(' * +Regex("[a-z]+") * -')' map { it.value }

fun main() {
    cleanResult.parseAllOrThrow("(hello)")  // => "hello"
}
```

Destructure tuples in `map` to transform results:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val pair = +Regex("[a-z]+") * -',' * +Regex("[0-9]+") map { (word, num) ->
    word.value to num.value.toInt()
}

fun main() {
    pair.parseAllOrThrow("hello,42")  // => ("hello", 42)
}
```

## Input Boundaries

`startOfInput` and `endOfInput` match at position boundaries without consuming input:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val word = +Regex("[a-z]+") map { it.value }

fun main() {
    val context = ParseContext("hello world", useMemoization = true)
    
    // Match word at start
    val atStart = (startOfInput * word).parseOrNull(context, 0)
    check(atStart?.value == "hello")  // Succeeds at position 0
    
    // Fails: position 6 is not at start
    val notAtStart = (startOfInput * word).parseOrNull(context, 6)
    check(notAtStart == null)  // Fails when not at start
}
```

**Note:** When using `parseAllOrThrow`, boundary checks are redundant—it already verifies the entire input is consumed. Use these parsers with `parseOrNull` or within sub-grammars.

## Naming Parsers

Assign names for better error messages:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val digit = (+Regex("[0-9]")) named "digit"
val letter = (+Regex("[a-z]")) named "letter"
val identifier = (letter * (letter + digit).zeroOrMore) named "identifier"

fun main() {
    try {
        identifier.parseAllOrThrow("123abc")
        error("Should have thrown exception")
    } catch (e: UnmatchedInputParseException) {
        // Error context references "identifier" and "letter"
        check(e.message!!.contains("Failed to parse"))
    }
}
```

### Named Composite Parsers

Named composite parsers hide constituent parsers from error suggestions:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

fun main() {
    val parserA = (+'a') named "letter_a"
    val parserB = (+'b') named "letter_b"
    
    // Named composite: only "ab_sequence" in errors
    val namedComposite = (parserA * parserB) named "ab_sequence"
    
    // Unnamed composite: "letter_a" in errors
    val unnamedComposite = parserA * parserB
    
    val context1 = ParseContext("c", useMemoization = true)
    context1.parseOrNull(namedComposite, 0)
    check(context1.suggestedParsers.map { it.name } == listOf("ab_sequence"))
    
    val context2 = ParseContext("c", useMemoization = true)
    context2.parseOrNull(unnamedComposite, 0)
    check(context2.suggestedParsers.map { it.name } == listOf("letter_a"))
}
```

**Best practice:** Name composite parsers for semantic errors ("Expected: identifier") and leave components unnamed for detailed token-level errors during development.

> **Tip:** Call parsers through `context.parseOrNull(parser, start)` rather than `parser.parseOrNull(context, start)` for proper named parser handling.

## Key Takeaways

- **`+`** for alternatives (first match wins)
- **`.optional`** rewinds on failure, returns `Tuple1<T?>`
- **`.zeroOrMore` / `.oneOrMore`** collect matches into lists
- **`-parser`** drops values from tuples
- **Destructuring** in `map` transforms tuple results
- **`startOfInput` / `endOfInput`** match boundaries
- **`named`** improves error messages

## Next Steps

Learn how to handle recursive grammars and operator precedence.

→ **[Step 3: Expressions & Recursion](03-expressions.html)**
