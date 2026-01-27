# Indent-Based Language Parser Sample

This sample demonstrates how to extend `ParseContext` to support indent-based languages (like Python).

## Overview

The example shows:

1. **Extended ParseContext**: `IndentParseContext` extends the base `ParseContext` to track indentation levels
2. **Indent Management**: A stack-based approach to managing nested indentation
3. **Indent-Aware Parsing**: Custom parsers that validate and consume indentation

## Key Concepts

### IndentParseContext

The `IndentParseContext` class extends `ParseContext` and adds:

- `currentIndent`: Returns the current required indentation level
- `pushIndent(indent)`: Pushes a new indentation level onto the stack
- `popIndent()`: Pops the current indentation level from the stack

### Custom Parse Function

Since the standard `parseAllOrThrow` creates a basic `ParseContext`, we provide a custom `parseAllWithIndentOrThrow` function that creates an `IndentParseContext` instead.

### Indent-Based Grammar

The sample implements a simple grammar:

```
fun <name>:
    <statement>
    <statement>
    ...
```

Where:
- Function definitions start with `fun` followed by a name and `:`
- The function body is indented
- Empty function bodies are allowed

## Running the Sample

```bash
./gradlew run
```

## Example Output

```
Example 1 parsed: [FunctionNode(name=hello, body=[ExpressionNode(value=world)])]
Example 2 parsed: [FunctionNode(name=test, body=[ExpressionNode(value=a), ExpressionNode(value=b), ExpressionNode(value=c)])]
Example 3 parsed: [FunctionNode(name=empty, body=[]), FunctionNode(name=another, body=[ExpressionNode(value=x)])]

All examples parsed successfully!
```

## Implementation Notes

1. **Making ParseContext Extensible**: The base `ParseContext` class was changed from `class` to `open class` to enable inheritance
2. **Local Maven**: This sample uses `mavenLocal()` to consume the locally published version of xarpeg with the `open` modifier
3. **Indentation Validation**: The parser validates that indented blocks have consistent indentation and proper nesting

## Related Issues

This sample addresses the requirements discussed in the issue about supporting indent-based languages.
