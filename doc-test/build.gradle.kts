import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

repositories {
    mavenCentral()
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(false)
    outputColorName.set("RED")
}

dependencies {
    implementation(project(":"))
}

val generatedSrc = layout.projectDirectory.dir("src/generated/kotlin")

tasks.register("generateSrc") {
    description = "Extracts Kotlin code blocks from README.md and docs into doc-test sources"
    group = "documentation"

    inputs.files(project.rootProject.file("README.md"), project.rootProject.fileTree("pages/docs/en") { include("**/*.md") })
    outputs.dir(generatedSrc)

    doLast {
        generatedSrc.asFile.deleteRecursively()
        val kotlinBlockRegex = Regex("""^[ \t]*```kotlin\s*(?:\r?\n)?(.*?)(?:\r?\n)?[ \t]*```""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        val projectDirFile = project.rootProject.projectDir
        val generatedPackageNames = mutableListOf<String>()
        val sourceFiles = (listOf(project.rootProject.file("README.md")) + project.rootProject.fileTree("pages/docs/en") { include("**/*.md") }.files)
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

        fun String.isIndented(): Boolean = startsWith(" ") || startsWith("\t")

        fun hasIndentedContentAfter(lines: List<String>, startIndex: Int): Boolean {
            var idx = startIndex
            while (idx < lines.size) {
                val candidate = lines[idx]
                if (candidate.isNotBlank()) return candidate.isIndented()
                idx++
            }
            return false
        }

        fun parseDeclarationBlock(lines: List<String>, startIndex: Int): Pair<List<String>, Int> {
            var braceCount = 0
            val declLines = mutableListOf<String>()
            var j = startIndex
            while (j < lines.size) {
                val declLine = lines[j]
                declLines.add(declLine)
                braceCount += declLine.count { it == '{' }
                braceCount -= declLine.count { it == '}' }
                j++
                if (braceCount > 0) continue
                val nextLine = lines.getOrNull(j) ?: break
                if (nextLine.isBlank()) {
                    declLines.add(nextLine)
                    j++
                    if (!hasIndentedContentAfter(lines, j)) break else continue
                }
                if (!nextLine.isIndented()) break
            }
            return declLines to j
        }

        sourceFiles
            .sortedBy { it.first }
            .forEach { (relativePath, sourceFile) ->
                val codeBlocks = kotlinBlockRegex.findAll(sourceFile.readText()).map { it.groupValues[1].trimEnd() }.toList()
                if (codeBlocks.isNotEmpty()) {
                    codeBlocks.forEachIndexed { index, block ->
                        val lines = block.lines()
                        val imports = mutableListOf<String>()
                        val declarations = mutableListOf<String>()
                        val statements = mutableListOf<String>()
                        var hasMain = false

                        var i = 0
                        while (i < lines.size) {
                            val line = lines[i]
                            val trimmed = line.trim()
                            val isMainDeclaration = trimmed.startsWith("fun main(")
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

                            if (isDeclaration) {
                                if (isMainDeclaration) {
                                    hasMain = true
                                }
                                val (declLines, nextIndex) = parseDeclarationBlock(lines, i)
                                declarations.addAll(declLines)
                                declarations.add("")
                                i = nextIndex
                                continue
                            } else {
                                statements.add(line)
                                i++
                            }
                        }

                        val hasExecutableStatements = statements.any { line ->
                            val trimmedLine = line.trim()
                            trimmedLine.isNotEmpty() &&
                                !trimmedLine.startsWith("//") &&
                                !trimmedLine.startsWith("const val ") &&
                                !trimmedLine.startsWith("val ") &&
                                !trimmedLine.startsWith("var ") &&
                                !trimmedLine.startsWith("fun ")
                        }

                        if (!hasMain) {
                            if (block.isGradleDslBlock()) {
                                println("Skipped (gradle block without fun main): ${relativePath}#$index")
                                return@forEach
                            } else {
                                throw GradleException("Code block at index $index in file $relativePath is missing fun main; add main() or mark as Gradle DSL.")
                            }
                        }

                        if (hasExecutableStatements) {
                            throw GradleException("Code block at index $index in file $relativePath contains executable top-level statements together with main(); wrap calls like println/parseAllOrThrow or assignments inside main() or another function.")
                        }

                        val basePackageSegment = sanitizeSegment(relativePath.replace("/", "_").replace(".", "_"))
                        val packageName = listOf(basePackageSegment, sanitizeSegment(index.toString()))
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
                        }
                        val outputFile = generatedSrc.file("${packageName.replace(".", "/")}/Test.kt").asFile
                        outputFile.parentFile.mkdirs()
                        outputFile.writeText(fileContent)
                        /*
                        println(
                            buildString {
                                appendLine("===== Doc-test block before (${relativePath}#$index) =====")
                                appendLine(block)
                                appendLine("===== Doc-test block after (${outputFile.absolutePath}) =====")
                                appendLine(fileContent)
                                appendLine("===== End =====")
                            }
                        )
                        */
                        println("Generated: ${outputFile.absolutePath}")
                    }
                } else {
                    println("Skipped (no Kotlin blocks): $relativePath")
                }
            }

            if (generatedPackageNames.isNotEmpty()) {
                val mainFile = generatedSrc.file("Main.kt").asFile
                mainFile.parentFile.mkdirs()
                val mainContent = buildString {
                    appendLine("fun main() {")
                    generatedPackageNames.forEach { packageName ->
                        appendLine("    try {")
                        appendLine("        println(\"--- Running $packageName.main ---\")")
                        appendLine("        $packageName.main()")
                        appendLine("    } catch (e: Throwable) {")
                        appendLine("        e.printStackTrace()")
                        appendLine("    }")
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

// Make build task depend on ktlintFormat
tasks.named("build") {
    dependsOn("ktlintFormat")
}

// Make ktlint format tasks depend on generateSrc
tasks.matching { it.name.startsWith("runKtlintFormat") }.configureEach {
    dependsOn("generateSrc")
}

// Make ktlint check tasks depend on generateSrc and format
tasks.matching { it.name.startsWith("runKtlintCheck") }.configureEach {
    dependsOn("generateSrc")
    mustRunAfter(tasks.matching { it.name.startsWith("runKtlintFormat") })
}
