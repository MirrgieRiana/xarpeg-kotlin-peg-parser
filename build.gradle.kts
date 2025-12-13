plugins {
    kotlin("multiplatform") version "2.2.20"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "io.github.mirrgieriana.xarpite"
version = System.getenv("VERSION") ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    // JVM target
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // JS target with module kind
    js(IR) {
        binaries.executable()
        nodejs()
    }

    // Native target for Linux x64
    linuxX64 {
        binaries {
            executable {
                entryPoint = "io.github.mirrgieriana.xarpite.xarpeg.main"
            }
        }
    }

    // Native target for Windows x64
    mingwX64 {
        binaries {
            executable {
                entryPoint = "io.github.mirrgieriana.xarpite.xarpeg.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("imported/src/commonMain/kotlin")
        }

        val commonTest by getting {
            kotlin.srcDir("imported/src/commonTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        val mingwX64Main by getting
        val mingwX64Test by getting
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("maven"))
        }
    }
}

// Dokka configuration for KDoc generation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("xarpeg-kotlin-peg-parser")
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    
    // Whitelist: Only process JVM source set by name
    dokkaSourceSets {
        configureEach {
            suppress.set(true)
        }
        named("commonMain") {
            suppress.set(false)
        }
    }
}

// Tuple generator task
tasks.register("generateTuples") {
    description = "Generates tuple source files and verifies they match imported files"
    group = "build"
    
    val outputDir = layout.projectDirectory.dir("src/generated/kotlin/io/github/mirrgieriana/xarpite/xarpeg").asFile
    val outputDirParsers = layout.projectDirectory.dir("src/generated/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers").asFile
    
    val tuplesKt = file("imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/Tuples.kt")
    val tupleParserKt = file("imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers/TupleParser.kt")
    
    val generatedTuplesKt = outputDir.resolve("Tuples.kt")
    val generatedTupleParserKt = outputDirParsers.resolve("TupleParser.kt")
    
    doLast {
        // Configuration: Maximum tuple size to generate
        val maxTupleSize = 5
        
        // Create output directories
        outputDir.mkdirs()
        outputDirParsers.mkdirs()
        
        // Matches package declarations including dotted names, backtick identifiers, and trailing line comments
        val packageRegex = Regex("^\\s*package\\s+([\\w.`]+(?:\\s*\\.\\s*[\\w.`]+)*)(?:\\s*//.*)?\\s*$")
        val packageSearchLimit = 10
        fun packageLineOf(file: File) = file.useLines { lines ->
            lines.take(packageSearchLimit).firstNotNullOfOrNull { line -> packageRegex.find(line)?.groupValues?.getOrNull(1) }
                ?.let { "package $it" }
        } ?: throw GradleException("Package declaration not found in ${file.absolutePath}")

        // Generate Tuples.kt programmatically
        val typeParams = listOf("A", "B", "C", "D", "E")
        val tuplesPackage = packageLineOf(tuplesKt)
        val tuplesContent = buildString {
            appendLine(tuplesPackage)
            appendLine()
            appendLine("object Tuple0")
            for (n in 1..maxTupleSize) {
                val params = typeParams.take(n)
                val typeParamStr = params.joinToString(", ") { "out $it" }
                val paramStr = params.mapIndexed { i, p -> "val ${p.lowercase()}: $p" }.joinToString(", ")
                appendLine("data class Tuple$n<$typeParamStr>($paramStr)")
            }
        }
        generatedTuplesKt.writeText(tuplesContent)
        println("Generated: ${generatedTuplesKt.absolutePath}")
        
        // Generate TupleParser.kt programmatically
        val tupleParserPackage = packageLineOf(tupleParserKt)
        val tupleParserContent = buildString {
            appendLine(tupleParserPackage)
            appendLine()
            appendLine("import io.github.mirrgieriana.xarpite.xarpeg.ParseResult")
            appendLine("import io.github.mirrgieriana.xarpite.xarpeg.Parser")
            appendLine("import io.github.mirrgieriana.xarpite.xarpeg.Tuple0")
            for (n in 1..maxTupleSize) {
                appendLine("import io.github.mirrgieriana.xarpite.xarpeg.Tuple$n")
            }
            appendLine("import kotlin.jvm.JvmName")
            appendLine()
            appendLine("// Parser to Tuple1Parser")
            appendLine()
            appendLine("operator fun <T : Any> Parser<T>.unaryPlus(): Parser<Tuple1<T>> = this map { a -> Tuple1(a) }")
            appendLine()
            appendLine()
            appendLine("// Parser Combination")
            appendLine()
            appendLine("/** パーサーの結合は純粋関数ではなく、位置にマッチしたり解析位置を進めたりする副作用があることに注意。 */")
            appendLine("fun <L : Any, R : Any, T : Any> combine(left: Parser<L>, right: Parser<R>, function: (L, R) -> T) = Parser { context, start ->")
            appendLine("    val resultL = context.parseOrNull(left, start) ?: return@Parser null")
            appendLine("    val resultR = context.parseOrNull(right, resultL.end) ?: return@Parser null")
            appendLine("    ParseResult(function(resultL.value, resultR.value), resultL.start, resultR.end)")
            appendLine("}")
            appendLine()
            appendLine()
            appendLine("// Tuple0Parser vs Tuple0Parser = Tuple0Parser")
            appendLine()
            appendLine("@JvmName(\"times00\")")
            appendLine("operator fun Parser<Tuple0>.times(other: Parser<Tuple0>) = combine(this, other) { _, _ -> Tuple0 }")
            appendLine()
            appendLine()
            appendLine("// Tuple0Parser vs X = X")
            appendLine()
            appendLine("@JvmName(\"times0P\")")
            appendLine("operator fun <A : Any> Parser<Tuple0>.times(other: Parser<A>) = combine(this, other) { _, b -> b }")
            appendLine()
            for (n in 1..maxTupleSize) {
                val params = typeParams.take(n)
                val typeParamStr = params.joinToString(", ") { "$it : Any" }
                appendLine("@JvmName(\"times0$n\")")
                appendLine("operator fun <$typeParamStr> Parser<Tuple0>.times(other: Parser<Tuple$n<${params.joinToString(", ")}>>) = combine(this, other) { _, b -> b }")
                appendLine()
            }
            appendLine()
            appendLine("// X vs Tuple0Parser = X")
            appendLine()
            appendLine("@JvmName(\"timesP0\")")
            appendLine("operator fun <A : Any> Parser<A>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }")
            appendLine()
            for (n in 1..maxTupleSize) {
                val params = typeParams.take(n)
                val typeParamStr = params.joinToString(", ") { "$it : Any" }
                appendLine("@JvmName(\"times${n}0\")")
                appendLine("operator fun <$typeParamStr> Parser<Tuple$n<${params.joinToString(", ")}>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }")
                appendLine()
            }
            appendLine()
            appendLine("// Parser vs Parser = Tuple2Parser")
            appendLine()
            appendLine("@JvmName(\"timesPP\")")
            appendLine("operator fun <A : Any, B : Any> Parser<A>.times(other: Parser<B>) = combine(this, other) { a, b -> Tuple2(a, b) }")
            appendLine()
            appendLine()
            appendLine("// Parser vs TupleNParser = Tuple(N+1)Parser")
            appendLine()
            for (n in 1..(maxTupleSize - 1)) {
                val resultN = n + 1
                val rightParams = typeParams.subList(1, n + 1)  // B, C, D, E (skip A)
                val resultParams = typeParams.take(resultN)
                val typeParamStr = resultParams.joinToString(", ") { "$it : Any" }
                val rightTupleAccess = (0 until n).map { i -> "b.${typeParams[i].lowercase()}" }.joinToString(", ")
                appendLine("@JvmName(\"timesP$n\")")
                appendLine("operator fun <$typeParamStr> Parser<A>.times(other: Parser<Tuple$n<${rightParams.joinToString(", ")}>>) = combine(this, other) { a, b -> Tuple$resultN(a, $rightTupleAccess) }")
                appendLine()
            }
            appendLine()
            appendLine("// TupleNParser vs Parser = Tuple(N+1)Parser")
            appendLine()
            for (n in 1..(maxTupleSize - 1)) {
                val resultN = n + 1
                val leftParams = typeParams.take(n)
                val resultParams = typeParams.take(resultN)
                val typeParamStr = resultParams.joinToString(", ") { "$it : Any" }
                val leftTupleAccess = leftParams.mapIndexed { i, _ -> "a.${typeParams[i].lowercase()}" }.joinToString(", ")
                appendLine("@JvmName(\"times${n}P\")")
                appendLine("operator fun <$typeParamStr> Parser<Tuple$n<${leftParams.joinToString(", ")}>>.times(other: Parser<${typeParams[n]}>) = combine(this, other) { a, b -> Tuple$resultN($leftTupleAccess, b) }")
                appendLine()
            }
            appendLine()
            appendLine("// TupleNParser vs TupleMParser = Tuple(N+M)Parser")
            appendLine()
            val combinations = mutableListOf<Triple<Int, Int, Int>>()
            for (leftN in 1..(maxTupleSize - 1)) {
                for (rightN in 1..(maxTupleSize - 1)) {
                    val resultN = leftN + rightN
                    if (resultN <= maxTupleSize) {
                        combinations.add(Triple(leftN, rightN, resultN))
                    }
                }
            }
            combinations.forEachIndexed { index, (leftN, rightN, resultN) ->
                val leftParams = typeParams.take(leftN)
                val rightParams = typeParams.subList(leftN, leftN + rightN)
                val resultParams = typeParams.take(resultN)
                val typeParamStr = resultParams.joinToString(", ") { "$it : Any" }
                val leftTupleAccess = leftParams.mapIndexed { i, _ -> "a.${typeParams[i].lowercase()}" }.joinToString(", ")
                val rightTupleAccess = rightParams.mapIndexed { i, _ -> "b.${typeParams[i].lowercase()}" }.joinToString(", ")
                appendLine("@JvmName(\"times$leftN$rightN\")")
                append("operator fun <$typeParamStr> Parser<Tuple$leftN<${leftParams.joinToString(", ")}>>.times(other: Parser<Tuple$rightN<${rightParams.joinToString(", ")}>>) = combine(this, other) { a, b -> Tuple$resultN($leftTupleAccess, $rightTupleAccess) }")
                if (index < combinations.size - 1) {
                    appendLine()
                    appendLine()
                } else {
                    appendLine()
                }
            }
        }
        generatedTupleParserKt.writeText(tupleParserContent)
        println("Generated: ${generatedTupleParserKt.absolutePath}")
        
        // Verify Tuples.kt
        val expectedTuplesContent = tuplesKt.readText()
        if (tuplesContent != expectedTuplesContent) {
            throw GradleException("Generated Tuples.kt does not match imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/Tuples.kt")
        }
        println("Verified: Tuples.kt matches imported file")
        
        // Verify TupleParser.kt
        val expectedTupleParserContent = tupleParserKt.readText()
        if (tupleParserContent != expectedTupleParserContent) {
            throw GradleException("Generated TupleParser.kt does not match imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers/TupleParser.kt")
        }
        println("Verified: TupleParser.kt matches imported file")
        
        println("All tuple files generated and verified successfully!")
    }
}
