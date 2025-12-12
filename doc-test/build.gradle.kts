plugins {
    kotlin("jvm") version "2.2.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

val generatedDocSrc = layout.projectDirectory.dir("src/main/kotlin")

tasks.register("generateDocSrc") {
    description = "Extracts Kotlin code blocks from README.md and docs into doc-test sources"
    group = "documentation"

    inputs.files(project.rootProject.file("README.md"), project.rootProject.fileTree("docs") { include("**/*.md") })
    outputs.dir(generatedDocSrc)

    doLast {
        generatedDocSrc.asFile.deleteRecursively()
        val kotlinBlockRegex = Regex("""^[ \t]*```kotlin\s*(?:\r?\n)?(.*?)(?:\r?\n)?[ \t]*```""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        val projectDirFile = project.rootProject.projectDir
        val sourceFiles = (listOf(project.rootProject.file("README.md")) + project.rootProject.fileTree("docs") { include("**/*.md") }.files)
            .map { sourceFile ->
                val relativePath = sourceFile.relativeTo(projectDirFile).path.replace('\\', '/')
                relativePath to sourceFile
            }

        sourceFiles
            .sortedBy { it.first }
            .forEach { (relativePath, sourceFile) ->
                val codeBlocks = kotlinBlockRegex.findAll(sourceFile.readText()).map { it.groupValues[1].trimEnd() }.toList()
                if (codeBlocks.isNotEmpty()) {
                    codeBlocks.forEachIndexed { index, originalBlock ->
                        val imports = linkedSetOf<String>()
                        val blockBody = originalBlock.lines().filterNot { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("import ")) {
                                imports.add(trimmed)
                                true
                            } else false
                        }.joinToString("\n").trim()

                        val fileContent = buildString {
                            appendLine("@file:Suppress(\"unused\")")
                            appendLine("package docsnippets")
                            appendLine()
                            imports.forEach { appendLine(it) }
                            if (imports.isNotEmpty()) appendLine()
                            appendLine(blockBody)
                        }
                        val blockFile = generatedDocSrc.file("${relativePath.replace("/", ".")}.block$index.kt").asFile
                        blockFile.parentFile.mkdirs()
                        blockFile.writeText(fileContent)
                        println("Generated: ${blockFile.absolutePath}")
                    }
                } else {
                    println("Skipped (no Kotlin blocks): ${relativePath}")
                }
            }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateDocSrc")
}
