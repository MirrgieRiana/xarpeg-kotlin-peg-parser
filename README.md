<div align="center">
  <img src="https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/actions/workflows/check.yml/badge.svg" alt="CI Status">
  <img src="https://img.shields.io/github/v/release/MirrgieRiana/xarpeg-kotlin-peg-parser" alt="GitHub Release">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/metadata/kotlin.json&cacheSeconds=500" alt="Kotlin Version">
</div>

<br>

<p align="center">
  <img src="https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/main/assets/xarpeg-logo.svg" alt="Xarpeg logo" width="400">
</p>

# Xarpeg: Kotlin PEG Parser

**Lightweight PEG-style parser combinators for Kotlin Multiplatform**

Xarpeg (/ˈʃɑrpɛɡ/) is a compact, operator-driven parser combinator library for Kotlin. It targets JVM, JS (Node.js), and Native platforms (Linux x64, Linux ARM64, Windows x64), works directly on raw input strings without tokenization, and includes built-in memoization for predictable backtracking performance.

## Why Xarpeg?

- **🎯 Intuitive DSL** - Operator-based syntax feels natural: `+` for literals/regex/choice, `*` for sequences, `-` to ignore tokens
- **📦 Multiplatform** - Write once, run on JVM, JS (IR/Node.js), and Native (Linux/Windows)
- **🔧 No Tokenizer Needed** - Parse directly from strings using character, string, and regex parsers
- **⚡ Built-in Memoization** - Automatic caching keeps backtracking predictable; disable for lower memory use
- **🎨 Type-Safe Results** - Sequences yield `Tuple0..Tuple16` so you explicitly control what's kept or dropped

---

## Quick Example

Here's a complete arithmetic expression parser in just a few lines:

```kotlin
import io.github.mirrgieriana.xarpite.xarpeg.*
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val brackets: Parser<Int> = -'(' * ref { root } * -')'
    val factor = number + brackets
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    check(expr.parseAllOrThrow("2*(3+4)") == 14)  // ✓ Evaluates to 14
}
```

**Key concepts:**
- `+'x'` / `+"xyz"` / `+Regex("...")` create parsers from literals or patterns
- `*` sequences parsers, returning typed tuples
- `-parser` matches but ignores the result (useful for delimiters)
- `+` (binary) tries alternatives in order
- `named` improves error messages
- `ref { }` enables recursive grammars
- `leftAssociative` handles operator precedence without manual recursion

**Best practices:**
- Use `+'x'` for single characters, not `+"x"`
- Use `+"xyz"` for fixed strings, not `+Regex("xyz")`
- Always use `named` with `Regex` parsers for better error messages

> 💡 **New to parser combinators?** Start with our [step-by-step tutorial](#-learn-xarpeg-step-by-step)!

---

## 📚 Learn Xarpeg Step by Step

Follow our comprehensive tutorial to master parser combinators:

1. **[Quickstart](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/01-quickstart.html)** - Your first parser in minutes
2. **[Combinators](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/02-combinators.html)** - Sequences, choices, repetition, and naming
3. **[Expressions & Recursion](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/03-expressions.html)** - Build recursive grammars with `ref { }`
4. **[Runtime Behavior](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/04-runtime.html)** - Errors, exceptions, and caching
5. **[Parsing Positions](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/05-positions.html)** - Extract location information with `mapEx`
6. **[Template Strings](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/06-template-strings.html)** - Parse embedded expressions naturally

**→ Start the Tutorial: [[English](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/)] [[日本語](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/ja/)]**

---

## 🎮 Try It Live

**[🚀 Launch Online Parser Demo](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/online-parser/)**

Interactive browser-based parser that demonstrates:
- Real-time parsing and evaluation
- Error reporting with suggestions
- Kotlin/JS multiplatform capabilities

---

## API Quick Reference

### Core Combinators

| Operation | Syntax | Description |
|-----------|--------|-------------|
| **Literals** | `+'x'`, `+"xyz"`, `+Regex("...")` | Create parsers from characters, strings, or regex |
| **Sequence** | `parserA * parserB` | Match parsers in order, return typed tuple |
| **Choice** | `parserA + parserB` | Try alternatives; first match wins |
| **Ignore** | `-parser` | Match but drop result from tuple |
| **Repetition** | `.zeroOrMore`, `.oneOrMore`, `.list(min, max)` | Collect matches into `List<T>` |
| **Serial** | `serial(p1, p2, ...)` | Parse multiple different parsers, return `List<T>` (no tuple limit) |
| **Optional** | `.optional` | Try to match; rewind on failure |
| **Transform** | `.map { ... }` | Convert parsed value to another type |
| **Position** | `.mapEx { ctx, result -> ... }` | Access context and position info |
| **Position** | `.result` | Get full `ParseResult<T>` with value and positions |
| **Lookahead** | `!parser` | Succeed if parser fails (zero width) |
| **Naming** | `parser named "name"` | Assign name for error messages |
| **Recursion** | `ref { parser }` | Forward reference for recursive grammars |
| **Associativity** | `leftAssociative(...)`, `rightAssociative(...)` | Build operator chains |
| **Boundaries** | `startOfInput`, `endOfInput` | Match at position boundaries |

### Parsing Methods

- **`parseAllOrThrow(input)`** - Parse entire input or throw exception
- **`parseOrNull(context, start)`** - Attempt parse at position; return `ParseResult<T>?`

### Error Handling

- **`UnmatchedInputParseException`** - No parser matched at the current position
- **`ExtraCharactersParseException`** - Trailing input remains after successful parse
- Both exceptions provide `context` with `errorPosition` and `suggestedParsers` for detailed error reporting

### Performance

- **Memoization** - Enabled by default (`useMemoization = true`); disable with `useMemoization = false` for lower memory usage
- **Backtracking** - Memoized results make repeated attempts predictable; alternatives backtrack automatically

---

## Examples & Use Cases

### Complete Examples

- **[JSON Parser](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/JsonParserTest.kt)** - Full JSON implementation with escape sequences, numbers, arrays, and nested objects
- **[Arithmetic Interpreter](samples/interpreter/)** - Expression parser with evaluation and error reporting
- **[Online Parser Demo](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/online-parser/)** - Interactive browser-based parser ([source](samples/online-parser/))

### Real-World Usage

**[Xarpite](https://github.com/MirrgieRiana/Xarpite)** - The original project from which Xarpeg was extracted. Demonstrates:
- Large-scale grammar design and organization
- Integration into a complete application
- Performance optimization with caching
- Complex parsing scenarios in production

### Running the Samples

Try the samples locally to see Xarpeg in action:

**Minimal JVM Sample:**
```bash
cd samples/minimal-jvm-sample && ./gradlew run
```

**Online Parser Sample:**
```bash
cd samples/online-parser && ./gradlew build
# Open samples/online-parser/build/site/index.html in your browser
```

**Arithmetic Interpreter:**
```bash
cd samples/interpreter && ./gradlew run --args='-e "2*(3+4)"'
```

---

## Installation

Add Xarpeg to your project using Gradle. Replace `<latest-version>` with the version from [Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases).

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-kotlinMultiplatform:<latest-version>")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "io.github.mirrgieriana:xarpeg-kotlinMultiplatform:<latest-version>"
}
```

> **Note:** The API may evolve as we refine the DSL. Pin a specific version for production use.

---

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development setup and workflow
- Building and testing across platforms
- Running samples and examples
- Code style guidelines

---

## Resources

- **Documentation** - Complete tutorial [[English](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/)] [[日本語](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/ja/)]
- **[API Reference](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/kdoc/)** - Browse source for KDoc
- **[Examples](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples)** - Sample applications
- **[Issues](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/issues)** - Bug reports and feature requests
- **[Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases)** - Version history

---

> **⚠️ Documentation Notice:**  
> The natural language descriptions throughout this documentation (including this README) were predominantly generated by AI and may contain inaccuracies. When in doubt, please verify against the actual source code and test the behavior directly.

---

## License

MIT License - See [LICENSE](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/LICENSE) for details.

---

<sub>Logo uses [Monaspace](https://github.com/githubnext/monaspace)</sub>
