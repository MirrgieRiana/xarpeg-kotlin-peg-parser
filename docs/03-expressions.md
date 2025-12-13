# Step 3: Handle expressions and recursion

Complex grammars need self-reference and associativity. This step shows how to define recursive parsers and use left/right associativity helpers.

## Expression parser with recursion

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() }
    val paren: Parser<Int> by lazy { (-'(' * parser { root } * -')') map { value -> value } }
    val factor = number + paren
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

expr.parseAllOrThrow("2*(3+4)") // => 14
```

- Resolve self-reference with `parser { ... }` or `by lazy`.
- `leftAssociative` / `rightAssociative` take a term parser, an operator parser, and a combiner, saving you from hand-written recursive descent.
- Operators are ordinary parsers, so handling whitespace or multi-character operators works the same way.

## Extending the pattern

- For multiple precedence levels, layer from high to low priority as shown above.
- Unary, prefix, or postfix operators can be handled by inserting preprocessing `map` steps before the associativity helpers.

Once recursion and associativity are in place, review runtime exceptions and caching behavior to round out your parser.  
→ [Step 4: Errors and runtime behavior](04-runtime.md)
