# xarpite.kotlin-peg-parser

**xarpite.kotlin-peg-parser: Lightweight PEG-style parser combinators for Kotlin Multiplatform**

`xarpite.kotlin-peg-parser` provides a compact, operator-driven parser combinator API. It targets JVM, JS (Node.js), and Linux x64, works directly on raw input strings (no tokenizer), and ships with opt-in caching to keep backtracking predictable.

## Features

- **Kotlin Multiplatform** - JVM, JS (IR/Node.js), and Native (Linux x64)
- **Operator-based DSL** - Unary `+` builds parsers from literals/regex, binary `+` expresses alternatives, `*` sequences tuples, `!` is negative lookahead, `-` ignores tokens
- **Tuple-centric results** - Sequence results are `Tuple0..Tuple5` so you can explicitly keep or drop intermediate values
- **Built-in cache** - Memoizes `(parser, position)` by default; toggle per parse call
- **No tokenizer** - Consume the source `String` directly with character, string, or regex parsers

---

## Documentation

### Links

- [GitHub Repository](https://github.com/MirrgieRiana/kotlin-peg-parser/) — Official repository for source code and issue tracking
- [GitHub Pages](https://mirrgieriana.github.io/kotlin-peg-parser) — Published site that serves the README and `docs` directory

See the published docs entry point at [docs/index.md](./docs/index.md). GitHub Pages hosts the README at the site root with the `docs` directory preserved for deeper content.

---

## Installation

Gradle coordinates follow the project metadata (`group = "io.github.mirrgieriana.xarpite"`, `version = "1.0.0-SNAPSHOT"`). Add the dependency as usual:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/kotlin-peg-parser/maven/maven") }
}

dependencies {
    implementation("io.github.mirrgieriana.xarpite:kotlin-peg-parser:1.0.3")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url "https://raw.githubusercontent.com/MirrgieRiana/kotlin-peg-parser/maven/maven" }
}

dependencies {
    implementation "io.github.mirrgieriana.xarpite:kotlin-peg-parser:1.0.3"
}
```

---

## Quick Start

The API lives under `mirrg.xarpite.parser` and its `parsers` helpers. Operator overloads keep grammars short while remaining explicit about what is kept or ignored.

```kotlin
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

// Simple arithmetic expression parser.
val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { match -> match.value.toInt() }
    val brackets: Parser<Int> by lazy { -'(' * parser { root } * -')' }
    val factor = number + brackets
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    println(expr.parseAllOrThrow("2*(3+4)")) // => 14
}
```

Key points in the example:

- The wildcard `parsers.*` import brings all operator overloads (`+`, `-`, `*`, `!`, `map`, etc.) into scope.
- `+'a'`, `+"abc"`, and `+Regex("...")` create parsers for characters, strings, and regex matches (`MatchResult`)—map them to the shape you need.
- `-parser` (for example, `-'('`) ignores the matched token and yields `Tuple0`, so you can drop delimiters.
- `*` sequences parsers and returns tuples (`Tuple1..Tuple5`), preserving the parts you care about.
- `leftAssociative`/`rightAssociative` build operator chains without manual recursion.
- `parseAllOrThrow` requires the entire input to be consumed; it throws on unmatched input or trailing characters.

---

## Core Concepts & Combinators

- **Parser<T>**: `fun interface` with `parseOrNull(context, start)`; parse helpers pass a `ParseContext` that handles caching.
- **Total parsing**: `parseAllOrThrow(src, useCache = true)` returns the parsed value or throws `UnmatchedInputParseException` (no match at start) / `ExtraCharactersParseException` (trailing input).
- **Sequences and tuples**: `*` chains parsers and returns `Tuple0..Tuple5`. Ignored pieces (`-parser`) collapse out of the tuple.
- **Alternatives**: `parserA + parserB` (or `or(...)`) tries options in order.
- **Repetition**: `parser.zeroOrMore`, `parser.oneOrMore`, or `parser.list(min, max)` collect results into `List<T>`.
- **Optional**: `parser.optional` yields `Tuple1<T?>` without consuming input on absence.
- **Mapping**: `parser map { ... }` transforms the parsed value.
- **Lookahead**: `!parser` succeeds only when the inner parser fails (does not consume input).
- **Recursion**: `parser { ... }` (delegation) or `by lazy` fields allow self-referential grammars.

---

## Memoization and Performance

`ParseContext` caches results per `(parser, position)` when `useCache = true` (the default in `parseAllOrThrow`). Disable caching with `useCache = false` if you need to reduce memory and your grammar does not backtrack heavily.

---

## Error Handling

- `UnmatchedInputParseException` — nothing matched at the current position.
- `ExtraCharactersParseException` — parsing succeeded but did not consume all input (reports the trailing position).

---

## Contributing to the Project

### Prerequisites

- JDK 11 or higher
- Gradle 9.2.1 (provided via the wrapper)

### Building & Testing

```bash
./gradlew check
```

Targets include JVM, JS (Node.js), and Linux x64. Native builds download Kotlin/Native toolchains from JetBrains; ensure outbound network access when running Native tasks.

### Running the Sample

A small Hello World app is available for quick verification:

```bash
./gradlew jvmJar
java -cp build/libs/kotlin-peg-parser-jvm-1.0.0-SNAPSHOT.jar mirrg.xarpite.peg.HelloWorldKt
```

A standalone Gradle sample that consumes the library via its Maven coordinate lives under `samples/hello`:

```bash
./gradlew publishKotlinMultiplatformPublicationToMavenLocal publishJvmPublicationToMavenLocal
(cd samples && ../gradlew run)
```

Alternatively, you can run the hello sample directly:

```bash
./gradlew publishKotlinMultiplatformPublicationToMavenLocal publishJvmPublicationToMavenLocal
(cd samples && ../gradlew :hello:jvmRun)
```

---

## Versioning

The current version is `1.0.0-SNAPSHOT`; the API may evolve while iterating on the operator-based DSL. Pin an explicit version when depending on this library.

---

## License

`xarpite.kotlin-peg-parser` is distributed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Real-World Examples

### Xarpite

**Xarpite** is a practical application built using this parser library. It demonstrates how `kotlin-peg-parser` can be used in real-world scenarios to parse complex grammars efficiently.

Xarpite serves as both:
- A **working example** of the parser library in action
- The **original project** from which this parser was extracted and generalized

The parser component that powers Xarpite was refined over time and eventually extracted into this standalone library to make it reusable across different Kotlin Multiplatform projects.

**Repository:** [github.com/MirrgieRiana/Xarpite](https://github.com/MirrgieRiana/Xarpite)

Exploring the Xarpite source code can provide additional insights into:
- Structuring larger parser grammars
- Integrating the parser into a complete application
- Performance optimization techniques with caching
- Handling complex parsing scenarios

---

## About

This library began as a parser component inside the Xarpite project and was extracted into a standalone, reusable PEG-style parser toolkit for Kotlin Multiplatform.
