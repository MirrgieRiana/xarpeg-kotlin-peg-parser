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
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

val sign = (+'+' + +'-').optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() }
val signedInt = sign * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

val repeatedA = (+'a').oneOrMore map { it.joinToString("") }

signedInt.parseAllOrThrow("-42") // => -42
signedInt.parseAllOrThrow("99")  // => 99
repeatedA.parseAllOrThrow("aaaa") // => "aaaa"
```

- `optional` always rewinds, so it will not block what comes after. Its return is `Tuple1`, so use `it.a` or destructure with `map { (value) -> ... }`.
- Repetition results can be processed immediately with `map`; here we join the characters into a string.

## Shaping sequence results

Results from `*` arrive as `TupleX`, so destructure in `map { (a, b, c) -> … }` to build the desired type.  
Drop delimiters or unneeded values with `-parser` to keep tuple arity small.

Next, handle recursion and associativity to build expression parsers with less code.  
→ [Step 3: Handle expressions and recursion](03-expressions.md)
