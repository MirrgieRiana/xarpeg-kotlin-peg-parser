# mirrg.xarpite.kotlin-peg-parser

**mirrg.xarpite.kotlin-peg-parser: Minimal PEG-style Parser DSL for Kotlin Multiplatform**

`mirrg.xarpite.kotlin-peg-parser` is a small PEG-style parser combinator library for Kotlin Multiplatform. It lets you write grammars as a Kotlin DSL, parse directly from raw input text (no tokenizer), and relies on built-in memoization to keep backtracking fast and predictable.

## Features

- **Kotlin Multiplatform** - Works on JVM, JS (Node.js), and Native (Linux x64)
- **PEG-style DSL** - Write grammars directly in Kotlin as a composable DSL
- **No pre-tokenization** - Parse directly from `String` using character and regex parsers
- **Built-in memoization** - Packrat-style caching for efficient backtracking
- **Small, focused API** - Only the primitives needed for typical PEG-style grammars

---

## Installation

Add the library to your project using Gradle:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("mirrg.xarpite:kotlin-peg-parser:0.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "mirrg.xarpite:kotlin-peg-parser:0.1.0"
}
```

---

## Quick Start

Here's a simple example that parses and evaluates arithmetic expressions:

```kotlin
import mirrg.xarpite.kotlinpeg.Grammar
import mirrg.xarpite.kotlinpeg.Parser
import mirrg.xarpite.kotlinpeg.peg
import mirrg.xarpite.kotlinpeg.text
import mirrg.xarpite.kotlinpeg.regex
import mirrg.xarpite.kotlinpeg.rule
import mirrg.xarpite.kotlinpeg.choice
import mirrg.xarpite.kotlinpeg.seq
import mirrg.xarpite.kotlinpeg.leftAssoc

fun main() {
    val grammar: Grammar<Int> = peg {
        val number: Parser<Int> =
            regex("[0-9]+").map { it.text.toInt() }

        val lparen = text("(")
        val rparen = text(")")
        val plus = text("+")
        val minus = text("-")
        val times = text("*")
        val div = text("/")

        val expr = rule<Int>("expr")
        val term = rule<Int>("term")
        val factor = rule<Int>("factor")

        factor.define(
            choice(
                number,
                seq(lparen, expr, rparen).map { it[1] }
            )
        )

        term.define(
            leftAssoc(
                factor,
                choice(times, div)
            ) { left, op, right ->
                when (op.text) {
                    "*" -> left * right
                    "/" -> left / right
                    else -> error("unreachable")
                }
            }
        )

        expr.define(
            leftAssoc(
                term,
                choice(plus, minus)
            ) { left, op, right ->
                when (op.text) {
                    "+" -> left + right
                    "-" -> left - right
                    else -> error("unreachable")
                }
            }
        )

        start(expr)
    }

    val result = grammar.parse("1+2*3-4")
    println(result)  // Outputs: 3
}
```

This example demonstrates the core workflow: define terminals (literals and patterns), build non-terminals using combinators, and specify a start rule.

---

## Core Concepts

### Parser

The fundamental abstraction is `Parser<T>`, which consumes input and either fails or produces a value of type `T`.

```kotlin
val integer: Parser<Int> =
    regex("[0-9]+").map { it.text.toInt() }
```

### Grammar and Rules

A `Grammar<T>` bundles rules together with a start rule:

```kotlin
val grammar: Grammar<String> = peg {
    val word: Parser<String> =
        regex("[A-Za-z]+").map { it.text }

    val space = regex("[ \t\r\n]+")
    val hello = rule<String>("hello")

    hello.define(
        seq(word, space, word)
            .map { parts -> parts[0] + " " + parts[2] }
    )

    start(hello)
}

val result = grammar.parse("hello world")
println(result)
```

Use `rule(name)` to create a placeholder for recursive rules, then `define(...)` to attach the parser expression.

### Combinators

Build complex parsers from simpler ones:

```kotlin
val a = text("a")
val b = text("b")

// Sequence: match a then b
val ab: Parser<String> =
    seq(a, b).map { it[0].text + it[1].text }

// Choice: match a or b
val aOrB: Parser<String> =
    choice(a, b).map { it.text }

// Repetition: match zero or more digits
val manyDigits: Parser<String> =
    regex("[0-9]").many().map { tokens ->
        tokens.joinToString(separator = "") { it.text }
    }
```

**Available combinators:**

- `text("...")` - Match exact text
- `regex("...")` - Match regular expression
- `seq(p1, p2, ...)` - Match sequence of parsers
- `choice(p1, p2, ...)` - Try parsers in order (first match wins)
- `many(p)` / `p.many()` - Match zero or more
- `many1(p)` - Match one or more
- `optional(p)` - Match zero or one
- `not(p)` - Negative lookahead (succeeds if p fails, without consuming input)
- `leftAssoc(term, op, combine)` - Left-associative binary operators

---

## Design Goals

This library prioritizes:

- **Minimal** - Keep the core small; provide only essential PEG primitives
- **PEG-style DSL** - Grammars as Kotlin code, not separate grammar files
- **No tokenizer** - Work directly on input strings with character and regex parsers
- **Built-in memoization** - Cache results per `(parser, position)` to prevent exponential backtracking
- **Multiplatform** - One grammar definition that runs on JVM, JS, and Native

---

## Memoization and Performance

The parser engine uses memoization to cache results per `(parser, offset)`, avoiding exponential time complexity with backtracking grammars.

Memoization is enabled by default:

```kotlin
val result = grammar.parse("some input")
```

You can customize parsing behavior if needed:

```kotlin
val config = ParseConfig(
    memoize = true
)

val result = grammar.parse("some input", config)
```

Disabling memoization reduces memory usage but may increase parse time for grammars with heavy backtracking.

---

## Error Handling

Inspect parse failures without throwing exceptions:

```kotlin
when (val result = grammar.tryParse("invalid input")) {
    is ParseResult.Success -> {
        println(result.value)
    }
    is ParseResult.Failure -> {
        println(result.diagnostic)
    }
}
```

Diagnostics include:
- Position of the furthest failure
- Set of expected tokens or patterns
- Fragment of input around the failure

---

## Contributing to the Project

### Prerequisites

- JDK 8 or higher
- Gradle 8.5 (included via wrapper)

### Building

Build the project:

```bash
./gradlew build
```

### Running Tests

Run all tests across all platforms:

```bash
./gradlew test
```

Run tests for specific targets:

```bash
./gradlew jvmTest        # JVM tests
./gradlew jsTest         # JavaScript tests
./gradlew linuxX64Test   # Native Linux tests
```

### Running the Sample

The repository includes a Hello World sample demonstrating basic usage. Run it on different platforms:

**JVM:**
```bash
./gradlew jvmJar
java -cp build/libs/kotlin-peg-parser-jvm-1.0.0-SNAPSHOT.jar mirrg.xarpite.peg.HelloWorldKt
```

**JavaScript (Node.js):**
```bash
./gradlew jsNodeDevelopmentRun
```

**Native Linux:**
```bash
./gradlew linuxX64Binaries
./build/bin/linuxX64/debugExecutable/kotlin-peg-parser.kexe
```

---

## Versioning

This library is currently in the `0.x` series. The DSL and public API may change between releases. Pin a specific version if you depend on particular API behavior.

---

## License

`mirrg.xarpite.kotlin-peg-parser` is distributed under the MIT License.

```text
MIT License

Copyright (c) 20xx Mirrg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

[See LICENSE file for full text]
```

See the [LICENSE](LICENSE) file for complete details.

---

## About

This library was originally developed as an internal parser component in the Xarpite project and later extracted and generalized into a standalone, reusable PEG-style parser DSL for Kotlin Multiplatform.
