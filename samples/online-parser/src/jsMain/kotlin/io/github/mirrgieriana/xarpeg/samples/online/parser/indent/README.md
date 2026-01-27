# Indent-Based Language Parser Example

This directory contains an example implementation demonstrating how to extend `ParseContext` to support indent-based languages (like Python).

## Important Note

**This example requires `ParseContext` to be declared as `open class`.** This change is included in this PR but is not yet available in the published xarpeg releases (version 5.0.0 and earlier).

## How to Use This Example

### Option 1: With Local Development Build

To test this example with the changes from this PR:

1. Build and publish the main project to your local Maven repository:
   ```bash
   cd ../../..  # Navigate to project root
   ./gradlew publishToMavenLocal
   ```

2. Temporarily modify `samples/online-parser/build.gradle.kts` to add `mavenLocal()`:
   ```kotlin
   repositories {
       mavenLocal()  // Add this line
       mavenCentral()
   }
   ```

3. Temporarily update `samples/libs.versions.toml` to use the development version:
   ```toml
   [versions]
   xarpeg = "latest"
   ```

4. Run the tests:
   ```bash
   cd samples/online-parser
   ./gradlew jsTest
   ```

### Option 2: Wait for Future Release

This example will work out-of-the-box once a version of xarpeg with the `open class ParseContext` change is published to Maven Central.

## What This Example Demonstrates

1. **IndentParseContext**: An extension of `ParseContext` that tracks indentation levels using a stack
2. **Indent-aware parsing**: Custom parsers that validate and consume indentation
3. **Empty block support**: Handling of empty indented blocks
4. **Multi-line parsing**: Parsing multiple statements at the same indentation level

## Example Usage

```kotlin
val input = """
fun hello:
    world

fun test:
    a
    b
    c
""".trimIndent()

val result = IndentParser.program.parseAllWithIndentOrThrow(input)
// Parses functions with indented bodies
```

## Files

- `IndentParseContext.kt` - The custom context implementation
- `IndentExample.kt` - Parser implementation and usage examples
- `../jsTest/.../IndentParserTest.kt` - Tests demonstrating the functionality
