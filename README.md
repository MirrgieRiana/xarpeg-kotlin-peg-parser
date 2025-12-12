<<<<<< copilot/create-hello-world-sample
# kotlin-peg-parser
mirrg.xarpite.kotlin-peg-parser: Minimal and Flexible PEG Parser for Kotlin Multiplatform

## Project Setup

This project uses Kotlin Multiplatform 2.2.20 with support for:
- **JVM** - Java Virtual Machine target
- **JS** - JavaScript with Node.js (IR compiler)
- **Native** - Linux x64 native target

## Building

Build the project:
```bash
./gradlew build
```

## Running Tests

Run all tests:
```bash
./gradlew test
```

Run tests for specific targets:
```bash
./gradlew jvmTest    # JVM tests
./gradlew jsTest     # JavaScript tests
./gradlew linuxX64Test  # Native Linux tests
```

## Running the Sample

Run the Hello World sample on different targets:

### JVM
```bash
./gradlew jvmJar
java -cp build/libs/kotlin-peg-parser-jvm-1.0.0-SNAPSHOT.jar mirrg.xarpite.peg.HelloWorldKt
```

### JavaScript (Node.js)
```bash
./gradlew jsNodeDevelopmentRun
```

### Native Linux
```bash
./gradlew linuxX64Binaries
./build/bin/linuxX64/debugExecutable/kotlin-peg-parser.kexe
```

## Requirements

- JDK 8 or higher
- Gradle 8.5 (included via wrapper)

## License

See [LICENSE](LICENSE) file for details.
=======
# mirrg.xarpite.kotlin-peg-parser

mirrg.xarpite.kotlin-peg-parser: Minimal PEG-style Parser DSL for Kotlin Multiplatform

`mirrg.xarpite.kotlin-peg-parser` is a small PEG-style parser combinator library for Kotlin Multiplatform.  
It lets you write grammars as a Kotlin DSL, parse directly from raw input text (no tokenizer), and relies on built-in memoization to keep backtracking fast and predictable.

- Kotlin Multiplatform (JVM / JS / Native)
- PEG-style parser DSL written in Kotlin
- No pre-tokenization: parse directly from `String`
- Built-in memoization (packrat-style) for efficient backtracking
- Small, focused API

---

## Installation

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

## Quick start

The following example parses and evaluates a tiny arithmetic language.

```kotlin
import mirrg.xarpite.kotlinpeg.Grammar
import mirrg.xarpite.kotlinpeg.Parser
import mirrg.xarpite.kotlinpeg.peg
import mirrg.xarpite.kotlinpeg.text
import mirrg.xarpite.kotlinpeg.regex
import mirrg.xarpite.kotlinpeg.rule
import mirrg.xarpite.kotlinpeg.choice
import mirrg.xarpite.kotlinpeg.seq
import mirrg.xarpite.kotlinpeg.many
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
    println(result)
}
```

This illustrates the overall style: define terminals, build non-terminals via combinators, and choose a start rule.

---

## Design goals

`mirrg.xarpite.kotlin-peg-parser` is designed with the following goals:

- **Minimal**  
  Keep the core small. Only provide the primitives needed to express typical PEG-style grammars.

- **PEG-style DSL**  
  Grammars are written in Kotlin as a composable DSL instead of in a separate PEG source format.

- **No tokenizer**  
  Work directly on the input `String`. You are free to use regular expressions and character parsers as needed.

- **Built-in memoization**  
  Results are cached per `(parser, position)` so that recursive grammars and backtracking stay predictable.

- **Multiplatform**  
  One common grammar definition that runs on JVM, JS, and Native.

---

## Core concepts

### Parser

The core abstraction is `Parser<T>`, which consumes input and either fails or produces a value of type `T`.

```kotlin
val integer: Parser<Int> =
    regex("[0-9]+").map { it.text.toInt() }
```

### Grammar and rules

A `Grammar<T>` bundles one or more rules together with a start rule.

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

`rule(name)` creates a placeholder for a recursive rule. `define(...)` attaches the actual parser expression to it.

### Combinators

Typical combinators include:

```kotlin
val a = text("a")
val b = text("b")

val ab: Parser<String> =
    seq(a, b).map { it[0].text + it[1].text }

val aOrB: Parser<String> =
    choice(a, b).map { it.text }

val manyDigits: Parser<String> =
    regex("[0-9]").many().map { tokens ->
        tokens.joinToString(separator = "") { it.text }
    }
```

Available building blocks include:

- `text("...")`  
- `regex("...")`  
- `seq(p1, p2, ...)`  
- `choice(p1, p2, ...)`  
- `many(p)` / `p.many()`  
- `many1(p)`  
- `optional(p)`  
- `not(p)` (negative lookahead)  
- `leftAssoc(term, op, combine)` for left-associative binary operators

Exact names may differ between versions; see the API reference for the definitive list.

---

## Memoization and performance

The parser engine supports memoization per `(parser, offset)` to avoid exponential blow-ups with naive backtracking.

By default memoization is enabled when parsing through the high-level `Grammar` API:

```kotlin
val result = grammar.parse("some input")
```

You can customize parsing through an explicit configuration if needed:

```kotlin
val config = ParseConfig(
    memoize = true
)

val result = grammar.parse("some input", config)
```

Disabling memoization can reduce memory usage at the cost of potentially worse time complexity on highly backtracking grammars.

---

## Error handling

When parsing fails, you can inspect failure details instead of throwing immediately.

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

Diagnostics typically include:

- The position of the furthest failure
- A set of expected tokens or patterns
- The actual fragment around the failure

The exact format of error messages is subject to improvement between minor versions.

---

## Multiplatform

`mirrg.xarpite.kotlin-peg-parser` is implemented as a Kotlin Multiplatform library.

Typical targets include:

- `jvm`
- `js` (Node and browser)
- Selected `native` targets

Check the build configuration and release notes for the current list of supported targets.

---

## Versioning

While the library is in the `0.x` series, the DSL and public API may change between releases.  
If you depend on a particular grammar style or API surface, pin a specific version in your build.

---

## License

`mirrg.xarpite.kotlin-peg-parser` is distributed under the MIT License.

```text
MIT License

Copyright (c) 20xx Mirrg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

See the `LICENSE` file for the full text.

---

## Origin

This library was originally developed as an internal parser component in the Xarpite project and later extracted and generalized into a standalone, reusable PEG-style parser DSL for Kotlin Multiplatform.
>>>>>> main
