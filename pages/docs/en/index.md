---
layout: docs-en
title: Xarpeg Tutorial - Learn Parser Combinators
---

# Xarpeg Tutorial

Learn to build powerful parsers with Kotlin. This tutorial guides you from basic concepts to advanced techniques, step by step.

## Prerequisites

- Basic Kotlin knowledge (functions, lambdas, classes)
- Familiarity with regular expressions (helpful but not required)
- IDE with Kotlin support for code completion

## Installation

Add Xarpeg to your `build.gradle.kts`:

### Kotlin Multiplatform Projects

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-kotlinMultiplatform:<latest-version>")
}
```

### JVM-Only Projects

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-jvm:<latest-version>")
}
```

### JS-Only Projects

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-js:<latest-version>")
}
```

### Other Platforms

For other platform-specific artifacts (Native targets, WASM), see:
**[Maven Central Repository](https://repo1.maven.org/maven2/io/github/mirrgieriana/)**

Find the latest version in [Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases).

---

## Tutorial Steps

### 1. [Quickstart](01-quickstart.html)
Build your first parser in minutes. Learn the basic syntax and run a simple key-value parser.

**What you'll learn:** Creating parsers from literals and regex, sequencing with `*`, ignoring tokens with `-`, transforming results with `map`

---

### 2. [Combinators](02-combinators.html)
Master the building blocks: sequences, choices, repetition, and optional parsing.

**What you'll learn:** Alternatives with `+`, repetition (`.zeroOrMore`, `.oneOrMore`), optionals, input boundaries, naming parsers for better error messages

---

### 3. [Expressions & Recursion](03-expressions.html)
Handle recursive grammars and operator precedence for expression parsing.

**What you'll learn:** Forward references with `ref { }`, left/right associativity, building arithmetic parsers, proper type declarations

---

### 4. [Runtime Behavior](04-runtime.html)
Understand how parsers handle errors, consume input, and use caching.

**What you'll learn:** Exception types, `ParseContext` error tracking, memoization control, debugging techniques

---

### 5. [Parsing Positions](05-positions.html)
Extract location information for better error messages and source mapping.

**What you'll learn:** Position tracking with `mapEx`, calculating line/column numbers, extracting matched text

---

### 6. [Template Strings](06-template-strings.html)
Parse complex nested structures without tokenization.

**What you'll learn:** Handling embedded expressions, context switching with PEG, recursive string/expression parsing

---

## Complete Examples

### JSON Parser
Full implementation handling all JSON types with escape sequences, nested structures, and comprehensive tests.

→ **[View JSON Parser Source](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/JsonParserTest.kt)**

Features:
- String escape sequences (`\"`, `\\`, `\n`, `\uXXXX`)
- Numbers (integers, decimals, scientific notation)
- Recursive arrays and objects with `ref { }`
- Custom separator handling

### Arithmetic Interpreter
Expression parser with evaluation and error reporting including line/column positions.

→ **[View Interpreter Source](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples/interpreter)**

Features:
- Four arithmetic operations with precedence
- Parentheses for grouping
- Division by zero error reporting with positions
- Command-line interface

### Online Parser Demo
Interactive browser-based parser demonstrating real-time parsing and evaluation.

→ **[Try Live Demo](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/online-parser/)** | **[View Source](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples/online-parser)**

---

## Additional Resources

### API Documentation
- **KDoc in IDE** - Use code completion for inline documentation
- **[Parser.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/Parser.kt)** - Core interface and helpers
- **[parsers package](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers)** - Combinator implementations

### Tests
- **[ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt)** - Comprehensive behavior examples
- **[ErrorContextTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ErrorContextTest.kt)** - Error tracking examples

### Real-World Usage
- **[Xarpite](https://github.com/MirrgieRiana/Xarpite)** - Production application using Xarpeg for complex grammar parsing

---

## Need Help?

- **[GitHub Issues](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/issues)** - Report bugs or request features
- **[Main README](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - Quick reference and overview
- **[Contributing Guide](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/CONTRIBUTING.md)** - Development setup
