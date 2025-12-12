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
