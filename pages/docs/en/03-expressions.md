---
layout: default
title: Step 3 – Expressions & Recursion
---

# Step 3: Expressions & Recursion

Build recursive grammars and handle operator precedence for expression parsing.

## The Challenge

Parsing expressions like `2*(3+4)` requires:
1. **Recursion** - Expressions can contain other expressions
2. **Precedence** - Multiplication binds tighter than addition
3. **Associativity** - `2+3+4` should group as `(2+3)+4`

Xarpeg provides `ref { }` for recursion and `leftAssociative`/`rightAssociative` helpers for operator precedence.

## Expression Parser Example

Here's a complete arithmetic expression parser with recursion and precedence:

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
    check(expr.parseAllOrThrow("2*(3+4)") == 14)
    check(expr.parseAllOrThrow("5+3*2") == 11)
}
```

## Understanding the Code

### Forward References with `ref { }`

The `paren` parser needs to reference `root`, which is defined later. Use `ref { }` to create a forward reference (example: `val paren: Parser<Int> = -'(' * ref { root } * -')'`).

**Important:** Properties using `ref` need explicit type declarations (`Parser<Int>`) for type resolution.

### Operator Precedence

Layer parsers from highest to lowest precedence:
- `val factor = number + paren` — Highest: numbers and parentheses
- `val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }` — Middle: multiplication
- `val add = leftAssociative(mul, -'+') { a, _, b -> a + b }` — Lowest: addition

Each level builds on the previous one, ensuring correct precedence:
- `5+3*2` parses as `5+(3*2)` not `(5+3)*2`
- `2*(3+4)` parses as `2*((3+4))`

### Associativity Helpers

`leftAssociative(term, operator) { left, op, right -> result }` builds left-associative operator chains.

This handles expressions like `2+3+4+5` as `((2+3)+4)+5` without explicit recursion.

**Parameters:**
- `term` - Parser for operands (numbers, sub-expressions, etc.)
- `operator` - Parser for the operator (typically with `-` to ignore it)
- Combiner function receives: left operand, operator result, right operand

Similarly, `rightAssociative` groups from the right: `2^3^4` → `2^(3^4)`.

## Avoiding `by lazy`

**Do NOT use `by lazy` for recursive parsers**—it causes infinite recursion. The `ref { }` mechanism already handles lazy evaluation.

Use `by lazy` only as a last resort for rare initialization errors unrelated to recursion.

## Multiple Precedence Levels

Extend the pattern for more operators:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'
    
    val factor = number + paren
    val power = rightAssociative(factor, -'^') { a, _, b -> 
        var result = 1
        repeat(b) { result *= a }
        result
    }
    val mulOp = (+'*' map { '*' }) + (+'/' map { '/' })
    val mul = leftAssociative(power, mulOp) { a, op, b ->
        if (op == '*') a * b else a / b
    }
    val addOp = (+'+' map { '+' }) + (+'-' map { '-' })
    val add = leftAssociative(mul, addOp) { a, op, b ->
        if (op == '+') a + b else a - b
    }
    val root = add
}.root

fun main() {
    check(expr.parseAllOrThrow("2^3^2") == 512)  // Right-associative: 2^(3^2)
    check(expr.parseAllOrThrow("10-3-2") == 5)   // Left-associative: (10-3)-2
}
```

## Unary Operators

Handle prefix/postfix operators with preprocessing:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'
    
    // Unary minus
    val unary: Parser<Int> = (-'-' * ref { unary } map { -it }) + number + paren
    
    val mul = leftAssociative(unary, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    check(expr.parseAllOrThrow("-5+3") == -2)
    check(expr.parseAllOrThrow("-(2+3)") == -5)
}
```

## Key Takeaways

- **`ref { }`** enables forward references for recursive grammars
- **Explicit types** required for properties using `ref`
- **`leftAssociative` / `rightAssociative`** handle operator chains
- **Layer precedence** from high to low (factor → multiply → add)
- **Never use `by lazy`** with recursive parsers

## Next Steps

Learn how parsers handle errors, caching, and provide debugging information.

→ **[Step 4: Runtime Behavior](04-runtime.html)**
