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
        val sourceFiles = (listOf(project.rootProject.file("README.md")) + project.rootProject.fileTree("docs") { include("**/*.md") }.files)
            .map { sourceFile ->
                val relativePath = sourceFile.relativeTo(projectDirFile).path.replace('\\', '/')
                relativePath to sourceFile
            }

        fun String.isGradleDslBlock(): Boolean =
            contains("repositories {") || contains("dependencies {") || contains("plugins {")

        sourceFiles
            .sortedBy { it.first }
            .forEach { (relativePath, sourceFile) ->
                val codeBlocks = kotlinBlockRegex.findAll(sourceFile.readText()).map { it.groupValues[1].trimEnd() }.toList()
                if (codeBlocks.isNotEmpty()) {
                    val imports = linkedSetOf<String>()
                    val defaultImports = listOf(
                        "import mirrg.xarpite.parser.Parser",
                        "import mirrg.xarpite.parser.parseAllOrThrow",
                        "import mirrg.xarpite.parser.parsers.*"
                    )

                    val sharedDeclarations = mutableListOf<String>()

                    val blockBodies = codeBlocks.mapIndexed { index, block ->
                        val lines = block.lines()
                        val isGradleDsl = block.isGradleDslBlock()

                        lines.forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("import ")) {
                                imports.add(trimmed)
                            }
                        }

                        val declarationLines = mutableListOf<String>()
                        val statementLines = mutableListOf<String>()

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
                                isNamedObject

                            if (trimmed.startsWith("import ")) {
                                i++
                                continue
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
                                    if (braceCount == 0 && declLine.trim().isNotEmpty()) break
                                }
                                sharedDeclarations.addAll(declLines)
                                sharedDeclarations.add("")
                                i = j
                            } else {
                                val contentLine = if (isGradleDsl && trimmed.isNotEmpty()) "// $line" else line
                                statementLines.add(contentLine)
                                i++
                            }
                        }

                        buildString {
                            appendLine("private object Block_$index {")
                            declarationLines.dropLastWhile { it.isEmpty() }.forEach { line ->
                                if (line.isNotEmpty()) {
                                    appendLine("    $line")
                                } else {
                                    appendLine()
                                }
                            }
                            val effectiveStatements = statementLines.dropLastWhile { it.isEmpty() }
                            if (effectiveStatements.isNotEmpty()) {
                                appendLine("    init {")
                                effectiveStatements.forEach { line ->
                                    if (line.isNotEmpty()) {
                                        appendLine("        $line")
                                    } else {
                                        appendLine()
                                    }
                                }
                                appendLine("    }")
                            }
                            appendLine("}")
                        }
                    }

                    imports.addAll(defaultImports)

                    val packageName = relativePath.split('/').joinToString(".") { segment ->
                        val sanitized = segment.replace(Regex("[^A-Za-z0-9]"), "_")
                        val normalized = sanitized.ifEmpty { "_" }
                        if (normalized.first().isDigit()) "_$normalized" else normalized
                    }

                    val fileContent = buildString {
                        appendLine("@file:Suppress(\"unused\", \"UNCHECKED_CAST\", \"CANNOT_INFER_PARAMETER_TYPE\")")
                        appendLine("package $packageName")
                        appendLine()
                        imports.forEach { appendLine(it) }
                        if (imports.isNotEmpty()) appendLine()
                        sharedDeclarations.dropLastWhile { it.isEmpty() }.forEach { line ->
                            appendLine(line)
                        }
                        if (sharedDeclarations.isNotEmpty()) appendLine()
                        blockBodies.forEachIndexed { idx, body ->
                            append(body)
                            if (idx != blockBodies.lastIndex) appendLine()
                        }
                    }
                    val outputFile = generatedSrc.file("${packageName.replace(".", "/")}/Test.kt").asFile
                    outputFile.parentFile.mkdirs()
                    outputFile.writeText(fileContent)
                    println("Generated: ${outputFile.absolutePath}")
                } else {
                    println("Skipped (no Kotlin blocks): ${relativePath}")
                }
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
