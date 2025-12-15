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

Use `startOfInput` and `endOfInput` to check position boundaries within a larger grammar. These are useful when parsing sub-expressions or when you need boundary checks in the middle of parsing:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

// Parse a word that may appear anywhere in the input
val word = +Regex("[a-z]+") map { it.value }

// Parse a word only at the start of input
val wordAtStart = startOfInput * word

// Parse a word only at the end of input
val wordAtEnd = word * endOfInput

fun main() {
    val context = ParseContext("hello world", useCache = true)
    
    // wordAtStart succeeds at position 0
    val result1 = wordAtStart.parseOrNull(context, 0)
    println(result1?.value) // => "hello"
    
    // wordAtStart fails at position 6 (not at start)
    val result2 = wordAtStart.parseOrNull(context, 6)
    println(result2) // => null
    
    // wordAtEnd fails at position 0 (not at end after matching)
    val result3 = wordAtEnd.parseOrNull(context, 0)
    println(result3) // => null
    
    // wordAtEnd succeeds at position 6 (at end after matching)
    val result4 = wordAtEnd.parseOrNull(context, 6)
    println(result4?.value) // => "world"
}
```

Both parsers return `Tuple0` and consume no input, so they compose cleanly: `Tuple0 * X = X`.

Here's an example showing how whitespace affects matching with boundary parsers:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val spaces = +Regex("\\s*") map { it.value }
val word = +Regex("[a-z]+") map { it.value }

// Pattern: optional spaces, then word must be at start and end
val strictWord = spaces * startOfInput * word * endOfInput * spaces map { it.b }

fun main() {
    // No whitespace - matches because word is at position 0 and ends at input end
    val result1 = strictWord.parseAllOrThrow("hello")
    println("No spaces: $result1") // => "hello"
    
    // Leading whitespace - fails because after consuming spaces, not at start
    try {
        strictWord.parseAllOrThrow("  hello")
    } catch (e: UnmatchedInputParseException) {
        println("Leading spaces: failed (not at start after consuming spaces)")
    }
    
    // Trailing whitespace - fails because after matching word, not at end
    try {
        strictWord.parseAllOrThrow("hello  ")
    } catch (e: UnmatchedInputParseException) {
        println("Trailing spaces: failed (not at end before consuming trailing spaces)")
    }
    
    // Both leading and trailing - fails
    try {
        strictWord.parseAllOrThrow("  hello  ")
    } catch (e: UnmatchedInputParseException) {
        println("Both spaces: failed")
    }
}
```

This demonstrates that `startOfInput` and `endOfInput` check the current position, not the original input boundaries. After consuming characters (like whitespace), you're no longer at the start or end.

> **Note**: When using `parseAllOrThrow`, these boundary checks are redundant because it already starts at position 0 and verifies the entire input is consumed. These parsers are most useful with `parseOrNull` or within complex grammars where you parse sub-expressions.

## Naming parsers for better error messages

Use the `named` infix function to assign meaningful names to parsers. Named parsers improve error reporting by providing clearer context when parsing fails:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

// Create named parsers for better error messages
val digit = (+Regex("[0-9]")) named "digit"
val letter = (+Regex("[a-z]")) named "letter"

// Named parsers work with all combinators
val identifier = (letter * (letter + digit).zeroOrMore) named "identifier"

fun main() {
    // Success case
    val result = identifier.parseAllOrThrow("x123")
    println(result) // Tuple2(Tuple1(MatchResult(value=x)), List(MatchResult(value=1), ...))
    
    // When parsing fails, the named parser helps identify what was expected
    try {
        identifier.parseAllOrThrow("123abc")
    } catch (e: UnmatchedInputParseException) {
        println("Failed: ${e.message}") 
        // The error context includes information about named parsers
    }
}
```

Parser names are particularly useful when building complex grammars with many alternatives:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

fun main() {
    val keyword = (+"if" + +"while" + +"for") named "keyword"
    val operator = (+"+" + +"-" + +"*" + +"/") named "operator"
    val number = (+Regex("[0-9]+")) named "number"

    // When parsing fails, error messages can reference these names
    val expression = (number * operator * number) named "binary_expression"
    
    println(expression.parseAllOrThrow("42+17"))
}
```

Named parsers compose naturally with all other combinators:
- Sequences: `(namedParser * otherParser)`
- Choices: `(namedParser + otherParser)`
- Repetition: `namedParser.oneOrMore`
- Mapping: `namedParser map { ... }`

### Named composite parsers and error messages

When you name a composite parser, it affects which parsers appear in error suggestions. A named composite parser hides its constituent parsers from error messages, showing only the composite name:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

fun main() {
    val parserA = (+'a') named "letter_a"
    val parserB = (+'b') named "letter_b"
    
    // Named composite - only "ab_sequence" appears in error suggestions
    val namedComposite = (parserA * parserB) named "ab_sequence"
    
    // Unnamed composite - "letter_a" appears in error suggestions
    val unnamedComposite = parserA * parserB
    
    val context1 = ParseContext("c", useCache = true)
    context1.parseOrNull(namedComposite, 0)
    println(context1.suggestedParsers.map { it.name }) // ["ab_sequence"]
    
    val context2 = ParseContext("c", useCache = true)
    context2.parseOrNull(unnamedComposite, 0)
    println(context2.suggestedParsers.map { it.name }) // ["letter_a"]
}
```

This behavior helps you control error message granularity:
- **Name composite parsers** for high-level semantic errors: `"Expected: identifier"` is clearer than `"Expected: letter"`
- **Leave composites unnamed** for detailed token-level errors when debugging or building new grammars

> **Note**: To get proper named parser handling, call parsers through `context.parseOrNull(parser, start)` rather than `parser.parseOrNull(context, start)`. The former ensures the `isInNamedParser` flag is set correctly.

> **Note**: Naming is optional and primarily benefits error reporting and debugging. It does not affect parsing behavior or performance significantly.

Next, handle recursion and associativity to build expression parsers with less code.  
→ [Step 3: Handle expressions and recursion](03-expressions.md)
