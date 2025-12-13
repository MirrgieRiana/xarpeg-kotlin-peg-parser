# Arithmetic Interpreter Sample

A simple arithmetic interpreter that demonstrates parsing and evaluation of mathematical expressions using the xarpeg parser library.

## Features

- Supports four arithmetic operations: `+`, `-`, `*`, `/`
- Supports parentheses for grouping
- Integer-only arithmetic
- Reports division by zero errors with line and column position information

## Usage

### Using Gradle

```bash
./gradlew run --args="-e <expression>"
```

### Using the installed distribution

```bash
./gradlew installDist
./build/install/interpreter/bin/interpreter -e "<expression>"
```

## Examples

```bash
# Basic arithmetic
./gradlew run --args='-e "2+3*4"'
# Output: 14

# With parentheses
./gradlew run --args='-e "(2+3)*4"'
# Output: 20

# Division
./gradlew run --args='-e "20/4"'
# Output: 5

# Division by zero (reports error with position)
./gradlew run --args='-e "10/0"'
# Output: Error: Division by zero at line 1, column 3

# Complex expression with division by zero
./gradlew run --args='-e "10+20/(5-5)"'
# Output: Error: Division by zero at line 1, column 6
```

## Implementation Details

The interpreter uses:
- **Lazy evaluation**: Operations are wrapped in thunks (lambdas) that are evaluated on demand
- **Position tracking**: Each operation tracks its position in the source text using `mapEx`
- **Precedence handling**: Uses `leftAssociative` for proper operator precedence (multiplication/division before addition/subtraction)
- **Error reporting**: Division by zero errors include the exact line and column where the division operator appears

## Code Structure

- `Main.kt`: Contains the parser definition, evaluation logic, and command-line interface
- Uses the xarpeg parser library for parsing
- Implements a recursive descent parser with operator precedence
