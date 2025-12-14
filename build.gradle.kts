
plugins {
    kotlin("multiplatform") version "2.2.20"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
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

    // WASM target for JavaScript
    wasmJs {
        binaries.executable()
        nodejs()
    }

    // Native target for Linux x64
    linuxX64()

    // Native target for Linux ARM64
    linuxArm64()

    // Native target for Windows x64
    mingwX64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("imported/src/commonMain/kotlin")
            kotlin.srcDir("src/generated/kotlin")
        }

        val commonTest by getting {
            kotlin.srcDir("imported/src/commonTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.0.1")
    android.set(false)
    outputColorName.set("RED")

    filter {
        exclude("**/build/**")
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

tasks.register("writeKotlinMetadata") {
    val kotlinVersion = providers.provider {
        kotlin.coreLibrariesVersion ?: error("Kotlin core libraries version is not set")
    }
    val outputFile = layout.buildDirectory.file("maven/metadata/kotlin.json")
    inputs.property("kotlinVersion", kotlinVersion)
    outputs.file(outputFile)

    doLast {
        val versionEscaped = kotlinVersion.get().replace("\"", "\\\"")
        val json = """{"schemaVersion":1,"label":"Kotlin","message":"$versionEscaped","color":"blue"}"""
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(json)
        }
    }
}

// Dokka configuration for KDoc generation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("Xarpeg KDoc")
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

    doLast {
        val iconSource = layout.projectDirectory.file("assets/xarpeg-icon.svg").asFile
        val iconTarget = outputDirectory.get().asFile.resolve("images/logo-icon.svg")
        iconTarget.parentFile.mkdirs()
        iconSource.copyTo(iconTarget, overwrite = true)
    }
}

// Tuple generator task
tasks.register("generateTuples") {
    description = "Generates tuple source files"
    group = "build"
    
    val outputDir = layout.projectDirectory.dir("src/generated/kotlin/io/github/mirrgieriana/xarpite/xarpeg").asFile
    val outputDirParsers = layout.projectDirectory.dir("src/generated/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers").asFile

    val generatedTuplesKt = outputDir.resolve("Tuples.kt")
    val generatedTupleParserKt = outputDirParsers.resolve("TupleParser.kt")
    
    doLast {
        // Configuration: Maximum tuple size to generate
        val maxTupleSize = 16
        
        // Create output directories
        outputDir.mkdirs()
        outputDirParsers.mkdirs()
        
        // Generate Tuples.kt programmatically
        val typeParams = (0 until maxTupleSize).map { index -> ('A'.code + index).toChar().toString() }
        val tuplesContent = buildString {
            appendLine("package io.github.mirrgieriana.xarpite.xarpeg")
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
        val tupleParserContent = buildString {
            appendLine("package io.github.mirrgieriana.xarpite.xarpeg.parsers")
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
                appendLine("@JvmName(\"times${leftN}_${rightN}\")")
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

        println("All tuple files generated successfully!")
    }
}

// Ensure Kotlin compilation tasks depend on generateTuples
tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>>().configureEach {
    dependsOn("generateTuples")
}
