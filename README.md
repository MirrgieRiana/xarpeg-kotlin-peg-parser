# Xarpeg: Kotlin PEG Parser

**Xarpeg: Kotlin PEG Parser - Lightweight PEG-style parser combinators for Kotlin Multiplatform**

Xarpeg (/ˈʃɑrpɛɡ/) provides a compact, operator-driven parser combinator API. It targets JVM, JS (Node.js), and Native (Linux x64, Windows x64), works directly on raw input strings (no tokenizer), and ships with opt-in caching to keep backtracking predictable.

---

## Features

- **Kotlin Multiplatform** - JVM, JS (IR/Node.js), and Native (Linux x64, Windows x64)
- **Operator-based DSL** - Unary `+` builds parsers from literals/regex, binary `+` expresses alternatives, `*` sequences tuples, `!` is negative lookahead, `-` ignores tokens
- **Tuple-centric results** - Sequence results are `Tuple0..Tuple5` so you can explicitly keep or drop intermediate values
- **Built-in cache** - Memoizes `(parser, position)` by default; toggle per parse call
- **No tokenizer** - Consume the source `String` directly with character, string, or regex parsers

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
    val brackets: Parser<Int> by lazy { (-'(' * parser { root } * -')') map { value -> value } }
    val factor = number + brackets
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    check(expr.parseAllOrThrow("2*(3+4)") == 14)
}
```

Key points in the example:

- The wildcard `parsers.*` import brings all operator overloads (`+`, `-`, `*`, `!`, `map`, etc.) into scope.
- `+'a'`, `+"abc"`, and `+Regex("...")` create parsers for characters, strings, and regex matches (`MatchResult`)—map them to the shape you need.
- `-parser` (for example, `-'('`) ignores the matched token and yields `Tuple0`, so you can drop delimiters.
- `*` sequences parsers and returns tuples (`Tuple1..Tuple5`), preserving the parts you care about.
- `leftAssociative`/`rightAssociative` build operator chains without manual recursion.
- `parseAllOrThrow` requires the entire input to be consumed; it throws on unmatched input or trailing characters.

> 💡 **Want to learn more?** Check out the [Tutorial section](#-tutorial---learn-step-by-step) below for a complete step-by-step guide!

---

## 📚 Tutorial - Learn Step by Step

Ready to build powerful parsers? Follow our structured tutorial guide to master Xarpeg from basics to advanced techniques:

### Step-by-Step Learning Path

1. **🚀 [Quickstart](./docs/01-quickstart.md)** - Build your first parser  
   Start here with a minimal DSL example and learn how to run it immediately.

2. **🔧 [Combinators](./docs/02-combinators.md)** - Combine parsers effectively  
   Master sequences, choices, repetition, and other core patterns to build complex grammars.

3. **🔁 [Expressions & Recursion](./docs/03-expressions.md)** - Handle recursive grammars  
   Learn to use `parser {}` / `by lazy` and leverage associativity helpers for expression parsing.

4. **⚙️ [Runtime Behavior](./docs/04-runtime.md)** - Understand errors and performance  
   Deep dive into exceptions, full consumption requirements, and cache control.

5. **📍 [Parsing Positions](./docs/05-positions.md)** - Access position information  
   Work with parsing positions using `mapEx` while keeping types simple.

6. **🔗 [Template Strings](./docs/06-template-strings.md)** - Parse without tokenization  
   Discover how PEG parsers naturally handle template strings with embedded expressions.

### Additional Resources

- **[Complete Tutorial Guide](./docs/index.md)** — Entry point for all tutorial content
- **[GitHub Pages](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser)** — Published documentation site
- **[GitHub Repository](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/)** — Source code and issue tracking

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

### Memoization and Performance

`ParseContext` caches results per `(parser, position)` when `useCache = true` (the default in `parseAllOrThrow`). Disable caching with `useCache = false` if you need to reduce memory and your grammar does not backtrack heavily.

---

### Error Handling

- `UnmatchedInputParseException` — nothing matched at the current position.
- `ExtraCharactersParseException` — parsing succeeded but did not consume all input (reports the trailing position).

---

## Real-World Examples

### Xarpite

**Xarpite** is a practical application built using Xarpeg. It demonstrates how this library can be used in real-world scenarios to parse complex grammars efficiently.

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

## Installation

Gradle coordinates follow the project metadata (`group = "io.github.mirrgieriana.xarpite"`, `version = "<latest-version>"`). Check [Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases) for the current value. Add the dependency as usual:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven") }
}

dependencies {
    implementation("io.github.mirrgieriana.xarpite:xarpeg-kotlin-peg-parser:<latest-version>")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url "https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven" }
}

dependencies {
    implementation "io.github.mirrgieriana.xarpite:xarpeg-kotlin-peg-parser:<latest-version>"
}
```

---

## Versioning

Use the latest version from [Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases) (replace `<latest-version>` in the snippets with the number shown there); the API may evolve while iterating on the operator-based DSL. Pin an explicit version when depending on this library.

---

## Contributing to the Project

### Prerequisites

- JDK 11 or higher
- Gradle 9.2.1 (provided via the wrapper)

### Building & Testing

For **day-to-day development**, run JVM tests only to avoid downloading large native toolchains:

```bash
./gradlew jvmTest
```

For **full multiplatform validation** (JVM, JS, Linux x64, Windows x64):

```bash
./gradlew check
```

Note: Native builds download Kotlin/Native toolchains from JetBrains (several hundred MB); ensure outbound network access when running Native tasks.

### Running the Sample

A small Hello World app is available for quick verification:

```bash
./gradlew jvmJar
java -cp build/libs/xarpeg-kotlin-peg-parser-jvm-<latest-version>.jar mirrg.xarpite.peg.HelloWorldKt # replace with the version shown on Releases
```

A standalone Gradle sample that consumes the library via its Maven coordinate lives under `samples/java-run`:

```bash
./gradlew publishKotlinMultiplatformPublicationToMavenLocal publishJvmPublicationToMavenLocal
(cd samples/java-run && ./gradlew run)
```

A browser-based online parser sample lives under `samples/online-parser`. Build it to emit `build/site/index.html` that imports the JS module:

```bash
(cd samples/online-parser && ./gradlew build)
# Open samples/online-parser/build/site/index.html in a browser
```

View the hosted version at https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/online-parser/.

---

## License

Xarpeg is distributed under the MIT License. See the [LICENSE](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/LICENSE) file for details.

---

## About

This library began as a parser component inside the Xarpite project and was extracted into a standalone, reusable PEG-style parser toolkit for Kotlin Multiplatform.
