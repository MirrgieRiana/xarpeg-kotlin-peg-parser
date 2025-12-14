---
layout: default
title: Step 3 – Expressions
---

# Step 3: Handle expressions and recursion

Complex grammars need self-reference and associativity. This step shows how to define recursive parsers and use left/right associativity helpers.

## Expression parser with recursion

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() }
    val paren: Parser<Int> = -'(' * ref { root } * -')' map { value -> value }
    val factor = number + paren
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    expr.parseAllOrThrow("2*(3+4)") // => 14
}
```

- Resolve forward references with `ref { ... }`, which creates a lazy reference to another parser that enables referencing the property itself or properties declared below it.
- When using `ref` to define a property, declare its type explicitly to help with type resolution (e.g., `val paren: Parser<Int> = ...`).
- `leftAssociative` / `rightAssociative` take a term parser, an operator parser, and a combiner, saving you from hand-written recursive descent.
- Operators are ordinary parsers, so handling whitespace or multi-character operators works the same way.

## About `by lazy` with recursive parsers

**In general, avoid using `by lazy` for recursive parser definitions.** Using `by lazy` can cause infinite recursion when combined with recursive parsers, making it unsuitable for standard recursive grammar definitions.

In rare cases where you encounter initialization errors that cannot be resolved by proper use of `ref { }` and type declarations, `by lazy` may serve as a workaround:

    val paren: Parser<Int> by lazy { (-'(' * ref { root } * -')') map { value -> value } }

This should only be used as a last resort when all other approaches fail, as it can introduce subtle issues with recursive definitions.

## Extending the pattern

- For multiple precedence levels, layer from high to low priority as shown above.
- Unary, prefix, or postfix operators can be handled by inserting preprocessing `map` steps before the associativity helpers.

Once recursion and associativity are in place, review runtime exceptions and caching behavior to round out your parser.  
→ [Step 4: Errors and runtime behavior](04-runtime.md)
