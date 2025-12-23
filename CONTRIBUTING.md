# Contributing to Xarpeg

Thank you for your interest in contributing to Xarpeg! This guide will help you get started with development.

## Prerequisites

- **JDK 17 or higher** - Required for building and testing (as specified in CI workflows)
- **Gradle 9.2.1** - Provided via the wrapper (automatically downloaded)

## Development Workflow

### Quick Development Cycle

For **day-to-day development**, run JVM tests only to avoid downloading large native toolchains (several hundred MB):

```bash
./gradlew jvmTest
```

This provides fast feedback for most changes since the library is written in common Kotlin code.

### Full Multiplatform Validation

For **complete verification** before submitting changes, run the full test suite across all platforms (JVM, JS, Linux x64, Windows x64):

```bash
./gradlew check
```

**Note:** Native builds download Kotlin/Native toolchains from JetBrains. Ensure you have:
- Several hundred MB of disk space available
- Outbound network access to JetBrains servers
- Patience for the first run (subsequent runs use cached toolchains)

## Project Structure

- `src/commonMain/` - Common Kotlin code for all platforms
- `src/commonTest/` - Common tests for all platforms
- `pages/docs/en/` - Tutorial documentation in English (published to GitHub Pages)
- `pages/docs/ja/` - Tutorial documentation in Japanese (published to GitHub Pages)
- `samples/` - Example applications

## Code Style

- Follow Kotlin coding conventions
- Run `./gradlew check` to verify code style and tests
- Add tests for new features
- Update documentation for user-facing changes

## Submitting Changes

1. Fork the repository
2. Create a feature branch
3. Make your changes with clear commit messages
4. Run `./gradlew check` to verify all tests pass
5. Submit a pull request with a description of your changes

## Documentation

When modifying user-facing functionality:

1. Update relevant tutorial pages in `pages/docs/en/` (and `pages/docs/ja/` if you can)
2. Update examples in README.md if applicable
3. Ensure code examples compile and run correctly
4. Add inline KDoc comments for new public APIs

## Getting Help

- Check existing [Issues](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/issues) for similar problems
- Review the Tutorial [[English](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/en/)] [[日本語](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/docs/ja/)] for usage patterns
- Open a new issue for bugs or feature requests
