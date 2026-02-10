---
layout: docs-en
title: Step 2 – Combinators
---

# Step 2: Combinators

Learn to combine parsers using sequences, choices, repetition, and more to build complex grammars.

## Core Combinators

### Choice with `+`

Try alternatives in order. The first match wins:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val keyword = (+"if" + +"while" + +"for") named "keyword"

fun main() {
    keyword.parseAllOrThrow("if")      // ✓ matches "if"
    keyword.parseAllOrThrow("while")   // ✓ matches "while"
}
```

### Optional Parsing

`optional` attempts to match but rewinds on failure. Returns `Tuple1<T?>`:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val sign = (+'+' map { '+' }) + (+'-' map { '-' })
val signOpt = sign.optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val signedInt = signOpt * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

fun main() {
    check(signedInt.parseAllOrThrow("-42") == -42)
    check(signedInt.parseAllOrThrow("99") == 99)
}
```

Use `it.a` to access the optional value, or destructure with `map { (value) -> ... }`.

#### Combining Optionals with Tuples

When combining multiple optional parsers using `*`, tuples are automatically flattened to contain nullable values directly:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val optA = (+'a').optional
val optB = (+'b').optional
val combined = optA * optB

fun main() {
    // Result type is Tuple2<Char?, Char?> (flattened)
    // NOT Tuple2<Tuple1<Char?>, Tuple1<Char?>> (nested)
    val result1 = combined.parseAllOrThrow("ab")
    check(result1.a == 'a')  // Direct access to nullable Char
    check(result1.b == 'b')
    
    val result2 = combined.parseAllOrThrow("a")
    check(result2.a == 'a')
    check(result2.b == null)  // Missing optional is null
}
```

This flattening makes optional combinations more ergonomic—you work with nullable types directly instead of nested tuples.

### Repetition

Collect multiple matches into a list:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val digits = (+Regex("[0-9]") map { it.value } named "digit").oneOrMore map { matches -> 
    matches.joinToString("")
}

val letters = (+Regex("[a-z]") map { it.value } named "letter").zeroOrMore map { matches -> 
    matches
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

### Serial Parsing

When you need to parse multiple different parsers of the same type in sequence without tuple limits, use `serial`:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val article = +"the" + +"a"
val adjective = +"quick" + +"lazy"
val noun = +"fox" + +"dog"

val phrase = serial(article, +" ", adjective, +" ", noun)

fun main() {
    check(phrase.parseAllOrThrow("the quick fox") == listOf("the", " ", "quick", " ", "fox"))
    check(phrase.parseAllOrThrow("a lazy dog") == listOf("a", " ", "lazy", " ", "dog"))
}
```

`serial` returns a `List<T>` and has no theoretical upper limit, unlike tuple parsers which are limited to 16 elements. Use it when:
- You have many parsers to combine (especially beyond tuple limits)
- You need a long natural language phrase with selectable parts
- You want a list result instead of a tuple

For repeating the same parser, use `.list()` or `.oneOrMore` instead.

## Shaping Results

Sequences with `*` return tuples. Use `-parser` to drop unneeded values:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

// Without dropping: Tuple3<Char, MatchResult, Char>
val word = +Regex("[a-z]+") named "word"
val withDelimiters = +'(' * word * +')'

// With dropping: MatchResult (just the middle value)
val cleanResult = -'(' * word * -')' map { it.value }

fun main() {
    cleanResult.parseAllOrThrow("(hello)")  // => "hello"
}
```

Destructure tuples in `map` to transform results:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val wordPart = +Regex("[a-z]+") named "word"
val numPart = +Regex("[0-9]+") named "number"
val pair = wordPart * -',' * numPart map { (word, num) ->
    word.value to num.value.toInt()
}

fun main() {
    pair.parseAllOrThrow("hello,42")  // => ("hello", 42)
}
```

## Input Boundaries

`startOfInput` and `endOfInput` match at position boundaries without consuming input:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val word = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    // Matches at start of input
    val atStart = (startOfInput * word).parseAllOrThrow("hello")
    check(atStart == "hello")  // Succeeds
}
```

**Note:** When using `parseAllOrThrow`, boundary checks are redundant—it already verifies the entire input is consumed. Use these parsers with `parseOrNull` or within sub-grammars.

## Naming Parsers

Assign names for better error messages:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val digit = +Regex("[0-9]") named "digit"
val letter = +Regex("[a-z]") named "letter"
val identifier = (letter * (letter + digit).zeroOrMore) named "identifier"

fun main() {
    val result = identifier.parseAll("123abc")
    val exception = result.exceptionOrNull() as? ParseException
    
    check(exception != null)  // Parsing fails
    check(exception.message!!.contains("Syntax Error"))
}
```

### Named Composite Parsers

Named composite parsers hide constituent parsers from error suggestions:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun main() {
    val parserA = +'a' named "letter_a"
    val parserB = +'b' named "letter_b"
    
    // Named composite: only "ab_sequence" in errors
    val namedComposite = (parserA * parserB) named "ab_sequence"
    
    // Unnamed composite: "letter_a" in errors
    val unnamedComposite = parserA * parserB
    
    val result1 = namedComposite.parseAll("c")
    val exception1 = result1.exceptionOrNull() as? ParseException
    val names1 = exception1?.context?.suggestedParsers?.mapNotNull { it.name } ?: emptyList()
    check(names1.contains("ab_sequence"))
    
    val result2 = unnamedComposite.parseAll("c")
    val exception2 = result2.exceptionOrNull() as? ParseException
    val names2 = exception2?.context?.suggestedParsers?.mapNotNull { it.name } ?: emptyList()
    check(names2.contains("letter_a"))
}
```

**Best practice:** Name composite parsers for semantic errors ("Expected: identifier") and leave components unnamed for detailed token-level errors during development.

### Hidden Parsers

Sometimes parsers need to be tracked internally but shouldn't clutter error suggestions. Use `.hidden` for parsers like whitespace that can appear anywhere:

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun main() {
    val whitespace = (+Regex("\\s+")).hidden
    val number = +Regex("[0-9]+") named "number" map { it.value.toInt() }
    val operator = (+'*' + +'+') named "operator"

    // Parser that optionally accepts whitespace
    val expr = number * whitespace.optional * operator * whitespace.optional * number

    val result = expr.parseAll("42abc")  // Fails: expected operator or number

    val exception = result.exceptionOrNull() as? UnmatchedInputParseException
    check(exception != null)

    val suggestions = exception.context.suggestedParsers.mapNotNull { it.name }
    // Contains meaningful parsers but not hidden whitespace
    check(suggestions.contains("operator") || suggestions.contains("number"))
    check(!suggestions.contains(""))
}
```

`.hidden` is equivalent to `named("")` - it sets the parser name to an empty string, which excludes it from error suggestions while still tracking it internally.

**Use case:** Apply to parsers that can appear anywhere (whitespace, comments) to keep error messages focused on meaningful tokens.

## Key Takeaways

- **`+`** for alternatives (first match wins)
- **`.optional`** rewinds on failure, returns `Tuple1<T?>`
- **`.zeroOrMore` / `.oneOrMore`** collect matches into lists
- **`-parser`** drops values from tuples
- **Destructuring** in `map` transforms tuple results
- **`startOfInput` / `endOfInput`** match boundaries
- **`named`** improves error messages
- **`.hidden`** excludes parsers from error suggestions

## Next Steps

Learn how to handle recursive grammars and operator precedence.

→ **[Step 3: Expressions & Recursion](03-expressions.html)**
