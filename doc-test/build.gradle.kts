import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

val generatedSrc = layout.projectDirectory.dir("src/generated/kotlin")

tasks.register("generateSrc") {
    description = "Extracts Kotlin code blocks from README.md and docs into doc-test sources"
    group = "documentation"

    inputs.files(project.rootProject.file("README.md"), project.rootProject.fileTree("docs") { include("**/*.md") })
    outputs.dir(generatedSrc)

    doLast {
        generatedSrc.asFile.deleteRecursively()
        val kotlinBlockRegex = Regex("""^[ \t]*```kotlin\s*(?:\r?\n)?(.*?)(?:\r?\n)?[ \t]*```""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        val projectDirFile = project.rootProject.projectDir
        val generatedPackageNames = mutableListOf<String>()
        val sourceFiles = (listOf(project.rootProject.file("README.md")) + project.rootProject.fileTree("docs") { include("**/*.md") }.files)
            .map { sourceFile ->
                val relativePath = sourceFile.relativeTo(projectDirFile).path.replace('\\', '/')
                relativePath to sourceFile
            }

        fun String.isGradleDslBlock(): Boolean =
            contains("repositories {") || contains("dependencies {") || contains("plugins {")

        fun sanitizeSegment(segment: String): String {
            val sanitized = segment.replace(Regex("[^A-Za-z0-9]"), "_")
            val normalized = sanitized.ifEmpty { "_" }
            return if (normalized.first().isDigit()) "_$normalized" else normalized
        }

        sourceFiles
            .sortedBy { it.first }
            .forEach { (relativePath, sourceFile) ->
                val codeBlocks = kotlinBlockRegex.findAll(sourceFile.readText()).map { it.groupValues[1].trimEnd() }.toList()
                if (codeBlocks.isNotEmpty()) {
                    codeBlocks.forEachIndexed { index, block ->
                        val lines = block.lines()
                        val isGradleDsl = block.isGradleDslBlock()

                        val imports = mutableListOf<String>()
                        val declarations = mutableListOf<String>()
                        val statements = mutableListOf<String>()
                        var hasMain = false

                        var i = 0
                        while (i < lines.size) {
                            val line = lines[i]
                            val trimmed = line.trim()
                            val isNamedObject = trimmed.startsWith("object ") &&
                                !trimmed.startsWith("object {") &&
                                !trimmed.startsWith("object:") &&
                                !line.contains(" = object ")
                            val isDeclaration = trimmed.startsWith("sealed ") ||
                                trimmed.startsWith("data class ") ||
                                trimmed.startsWith("class ") ||
                                trimmed.startsWith("interface ") ||
                                trimmed.startsWith("enum ") ||
                                trimmed.startsWith("const val ") ||
                                trimmed.startsWith("val ") ||
                                trimmed.startsWith("var ") ||
                                trimmed.startsWith("fun ") ||
                                isNamedObject

                            if (trimmed.startsWith("import ")) {
                                imports.add(trimmed)
                                i++
                                continue
                            }

                            if (trimmed.startsWith("fun main(")) {
                                hasMain = true
                            }

                            if (isDeclaration) {
                                var braceCount = 0
                                val declLines = mutableListOf<String>()
                                var j = i
                                while (j < lines.size) {
                                    val declLine = lines[j]
                                    declLines.add(declLine)
                                    braceCount += declLine.count { it == '{' }
                                    braceCount -= declLine.count { it == '}' }
                                    j++
                                    if (braceCount > 0) continue
                                    if (j >= lines.size) break
                                    val nextLine = lines[j]
                                    if (nextLine.isBlank()) {
                                        val nextNonBlank = lines.asSequence()
                                            .drop(j + 1)
                                            .firstOrNull { it.isNotBlank() }
                                        val continueAfterBlank = nextNonBlank?.let { it.startsWith(" ") || it.startsWith("\t") } ?: false
                                        declLines.add(nextLine)
                                        j++
                                        if (!continueAfterBlank) break else continue
                                    }
                                    val isNextIndented = nextLine.startsWith(" ") || nextLine.startsWith("\t")
                                    if (!isNextIndented) break
                                }
                                declarations.addAll(declLines)
                                declarations.add("")
                                i = j
                            } else {
                                val contentLine = if (isGradleDsl && trimmed.isNotEmpty()) "// $line" else line
                                statements.add(contentLine)
                                i++
                            }
                        }

                        val hasExecutableStatements = statements.any { line ->
                            val trimmedLine = line.trim()
                            trimmedLine.isNotEmpty() && !trimmedLine.startsWith("//")
                        }

                        if (hasMain && hasExecutableStatements) {
                            throw GradleException("Code block $relativePath#$index contains top-level statements alongside main(); move them into main.")
                        }

                        val pathHashSegment = sanitizeSegment(relativePath.hashCode().toUInt().toString(36))
                        val packageName = (relativePath.split('/') + pathHashSegment + index.toString())
                            .map(::sanitizeSegment)
                            .joinToString(".")

                        generatedPackageNames.add(packageName)

                        val fileContent = buildString {
                            appendLine("@file:Suppress(\"unused\", \"UNCHECKED_CAST\", \"CANNOT_INFER_PARAMETER_TYPE\")")
                            appendLine("package $packageName")
                            appendLine()
                            imports.forEach { appendLine(it) }
                            if (imports.isNotEmpty()) appendLine()
                            declarations.dropLastWhile { it.isEmpty() }.forEach { line ->
                                appendLine(line)
                            }
                            if (declarations.isNotEmpty()) appendLine()
                            if (!hasMain) {
                                appendLine("fun main() {")
                                statements.dropLastWhile { it.isEmpty() }.forEach { line ->
                                    if (line.isNotEmpty()) {
                                        appendLine("    $line")
                                    } else {
                                        appendLine()
                                    }
                                }
                                appendLine("}")
                            }
                        }
                        val outputFile = generatedSrc.file("${packageName.replace(".", "/")}/Test.kt").asFile
                        outputFile.parentFile.mkdirs()
                        outputFile.writeText(fileContent)
                        println("Generated: ${outputFile.absolutePath}")
                    }
                } else {
                    println("Skipped (no Kotlin blocks): ${relativePath}")
                }
            }

        if (generatedPackageNames.isNotEmpty()) {
            val mainFile = generatedSrc.file("Main.kt").asFile
            mainFile.parentFile.mkdirs()
            val mainContent = buildString {
                appendLine("fun main() {")
                generatedPackageNames.forEach { packageName ->
                    appendLine("    println(\"--- Running $packageName.main ---\")")
                    appendLine("    $packageName.main()")
                    appendLine()
                }
                appendLine("}")
            }
            mainFile.writeText(mainContent)
            println("Generated runner: ${mainFile.absolutePath}")
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateSrc")
}

sourceSets {
    named("main") {
        kotlin.srcDir(generatedSrc)
    }
}

val runDocSamples = tasks.register<JavaExec>("runDocSamples") {
    dependsOn("compileKotlin")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("MainKt")
}

tasks.named("check") {
    dependsOn(runDocSamples)
}
