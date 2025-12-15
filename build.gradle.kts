
import build_logic.getTupleParserSrc
import build_logic.getTupleSrc

plugins {
    kotlin("multiplatform") version "1.9.20"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("build-logic")
}

group = "io.github.mirrgieriana.xarpite"
val SHORT_SHA_LENGTH = 7
val MAX_SHA_LENGTH = 40
val gitShaRegex = Regex("^[0-9a-fA-F]{${SHORT_SHA_LENGTH},${MAX_SHA_LENGTH}}$")

fun isValidGitSha(sha: String): Boolean = gitShaRegex.matches(sha)

fun Project.readGitSha(): String? = runCatching {
    val process = ProcessBuilder("git", "rev-parse", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
    process.waitFor()
    output.takeIf(::isValidGitSha)
}.getOrNull()

fun Project.determineVersion(): String {
    System.getenv("VERSION")?.let { return it }
    val sanitizedSha = readGitSha()
    return sanitizedSha?.let { "latest-commit-${it.take(SHORT_SHA_LENGTH)}" } ?: "latest"
}

version = project.determineVersion()

repositories {
    mavenCentral()
}

kotlin {
    // JVM target
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
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
    // Note: Removed @OptIn(ExperimentalWasmDsl) for Kotlin 1.9.20 compatibility
    // The annotation class doesn't exist in this version, though WASM is experimental
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
            kotlin.srcDir("src/importedMain/kotlin")
            kotlin.srcDir("src/generated/kotlin")
        }

        val commonTest by getting {
            kotlin.srcDir("src/importedTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(false)
    outputColorName.set("RED")

    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
        exclude("src/importedMain/**")
        exclude("src/importedTest/**")
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("maven/maven"))
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
    moduleName.set(providers.gradleProperty("repositoryName"))
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    
    // Whitelist: Only process JVM source set by name
    dokkaSourceSets {
        configureEach {
            suppress.set(true)
        }
        named("commonMain") {
            suppress.set(false)
            // Explicitly include generated sources for Tuple classes
            sourceRoots.from(layout.projectDirectory.dir("src/generated/kotlin"))
        }
    }

    doLast {
        val iconSource = layout.projectDirectory.file("assets/xarpeg-icon.svg").asFile
        val iconTarget = outputDirectory.get().asFile.resolve("images/logo-icon.svg")
        iconTarget.parentFile.mkdirs()
        iconSource.copyTo(iconTarget, overwrite = true)
    }
    
    // Ensure generated sources are available before Dokka runs
    dependsOn("generateTuples")
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
        generatedTuplesKt.writeText(getTupleSrc(maxTupleSize))
        println("Generated: ${generatedTuplesKt.absolutePath}")
        
        // Generate TupleParser.kt programmatically
        generatedTupleParserKt.writeText(getTupleParserSrc(maxTupleSize))
        println("Generated: ${generatedTupleParserKt.absolutePath}")

        println("All tuple files generated successfully!")
    }
}

// Ensure Kotlin compilation tasks depend on generateTuples
tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>>().configureEach {
    dependsOn("generateTuples")
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("$projectDir/detekt.yml"))

    source.setFrom(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmMain/kotlin",
        "src/jvmTest/kotlin",
        "src/jsMain/kotlin",
        "src/jsTest/kotlin",
        "src/importedMain/kotlin",
        "src/importedTest/kotlin"
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(true)
    }
}

// Add detekt to the check task
tasks.named("check") {
    dependsOn("detekt")
}
