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
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'
    val factor = number + paren
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    expr.parseAllOrThrow("2*(3+4)") // => 14
}
```

- **Use `ref { }` for forward references**: When a property needs to reference itself or properties declared below it, wrap the reference with `ref { }` to enable lazy reference resolution.
- **Add explicit type declarations**: Properties that use `ref` should have explicit type declarations (like `Parser<Int>`) to help with type resolution.
- **Avoid `by lazy` for recursive parsers**: Using `by lazy` on recursive parser properties causes infinite recursion and should be avoided.
- `leftAssociative` / `rightAssociative` take a term parser, an operator parser, and a combiner, saving you from hand-written recursive descent.
- Operators are ordinary parsers, so handling whitespace or multi-character operators works the same way.

## Important: Avoid `by lazy` for recursive parsers

**Do not use `by lazy` for recursive parsers** as it causes infinite recursion. The `ref { }` mechanism already handles lazy evaluation internally, making `by lazy` unnecessary and problematic.

However, in rare situations (not related to recursion), you may encounter unreasonable initialization errors. In these exceptional cases only, `by lazy` can be used as a last resort workaround:

    val paren: Parser<Int> by lazy { -'(' * ref { root } * -')' }

This is an advanced workaround that should only be used when you encounter specific initialization errors, not as a standard pattern for recursive parsers.

## Extending the pattern

- For multiple precedence levels, layer from high to low priority as shown above.
- Unary, prefix, or postfix operators can be handled by inserting preprocessing `map` steps before the associativity helpers.

Once recursion and associativity are in place, review runtime exceptions and caching behavior to round out your parser.  
→ [Step 4: Errors and runtime behavior](04-runtime.md)
