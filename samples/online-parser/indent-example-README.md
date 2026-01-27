# Indent-Based Language Support Example

This directory contains an example implementation showing how to extend `ParseContext` to support indent-based languages (like Python).

## Important Note

**This example requires `ParseContext` to be declared as `open class`.** This change is part of this PR but is not available in published versions of xarpeg yet (version 5.0.0 uses final `class ParseContext`).

## Files

- **`IndentParseContext.kt`**: Extended `ParseContext` with indent level tracking using a stack-based approach
- **`IndentExample.kt`**: Complete parser implementation for a simple indent-based language with function definitions
- **`../jsTest/.../IndentParserTest.kt`**: Test suite demonstrating the functionality

## What This Demonstrates

The example shows how to parse a simple indent-based language with:

```
fun hello:
    world

fun test:
    a
    b
    c

fun empty:
fun another:
    x
```

### Key Features

1. **Stack-based indent management**: `IndentParseContext` tracks indentation levels with push/pop operations
2. **Empty block support**: Functions can have empty bodies (like `fun empty:`)
3. **Consistent indentation validation**: All lines in a block must have the same indentation
4. **Proper nesting**: Nested blocks must have deeper indentation than their parent

## Usage

Once `open class ParseContext` is available in a published version, this example can be integrated into the online parser demo by:

1. Adding the `parseIndentCode` function to the imports in `index.html`
2. Adding example cards to the sidebar for indent-based code
3. Users can then try indent-based parsing interactively

## Testing Locally

To test this example with the current development version:

1. Build and publish to local Maven:
   ```bash
   cd /path/to/xarpeg-kotlin-peg-parser
   ./gradlew publishToMavenLocal
   ```

2. Run the tests:
   ```bash
   cd samples/online-parser
   ./gradlew jsTest
   ```

The tests in `IndentParserTest.kt` validate all the functionality.
