import build_logic.getTupleParserSrc
import build_logic.getTupleSrc

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    id("build-logic")
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = libs.versions.xarpeg.group.get()
version = libs.versions.xarpeg.version.get()

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

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
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
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
            kotlin.srcDir("src/generated/kotlin")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(libs.versions.ktlint.asProvider().get())
    android.set(false)
    outputColorName.set("RED")
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            val ownerName = providers.gradleProperty("ownerName").get()
            val repositoryName = providers.gradleProperty("repositoryName").get()
            val repositoryFullName = "$ownerName/$repositoryName"

            name = libs.versions.xarpeg.name.get()
            description = "Lightweight PEG-style parser combinators for Kotlin Multiplatform"
            url = "https://github.com/$repositoryFullName"
            licenses {
                license {
                    name = "MIT License"
                    url = "http://www.opensource.org/licenses/mit-license.php"
                }
            }
            developers {
                developer {
                    id = providers.gradleProperty("developer").get()
                    name = providers.gradleProperty("developerMavenId").get()
                    url = "https://github.com/$ownerName"
                }
            }
            scm {
                connection = "scm:git:https://github.com/$repositoryFullName.git"
                developerConnection = "scm:git:ssh://github.com:$repositoryFullName.git"
                url = "https://github.com/$repositoryFullName"
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("maven/maven"))
        }
    }
}

signing {
    isRequired = true

    val signingKey = providers.gradleProperty("signingKey").orNull
    val signingPassword = providers.gradleProperty("signingPassword").orNull
    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
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
        "src/jsTest/kotlin"
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

// Make build task depend on ktlintFormat
tasks.named("build") {
    dependsOn("ktlintFormat")
}

// Make ktlint check tasks run after format tasks
tasks.matching { it.name.startsWith("runKtlintCheck") }.configureEach {
    mustRunAfter(tasks.matching { it.name.startsWith("runKtlintFormat") })
}

// Generate documentation social image
val generateDocsSocialImage = tasks.register("generateDocsSocialImage") {
    group = "build"
    description = "Generates social image for documentation pages using Playwright"

    val templateSource = file("pages/social-image-template.html")
    val iconSource = file("assets/xarpeg-icon.svg")
    val intermediateDir = layout.buildDirectory.dir("socialImage").get().asFile
    val templateIntermediate = intermediateDir.resolve("social-image-template.html")
    val iconIntermediate = intermediateDir.resolve("xarpeg-icon.svg")
    val outputImage = intermediateDir.resolve("social-image.png")

    inputs.files(templateSource, iconSource)
    outputs.file(outputImage)

    doLast {
        // Create intermediate directory
        intermediateDir.mkdirs()

        // Copy template and icon to intermediate directory
        templateSource.copyTo(templateIntermediate, overwrite = true)
        iconSource.copyTo(iconIntermediate, overwrite = true)

        // Generate social image using Playwright
        build_logic.generateSocialImageWithPlaywright(
            htmlTemplate = templateIntermediate,
            outputFile = outputImage
        )

        println("Documentation social image generated: ${outputImage.absolutePath}")
    }
}

// Bundle all Pages content for deployment
val bundleRelease = tasks.register<Sync>("bundleRelease") {
    group = "build"
    description = "Bundles all Pages content (pages/, online-parser, dokka) into build/bundleRelease for Jekyll processing"

    val outputDirectory = layout.buildDirectory.dir("bundleRelease")

    dependsOn("dokkaHtml", generateDocsSocialImage)

    into(outputDirectory)

    // Copy pages directory content
    from("pages") {
        exclude("_site/**", ".jekyll-cache/**", "social-image-template.html", "vendor/**", ".bundle/**", "Gemfile.lock")
    }

    // Copy online-parser build output (built separately)
    from("samples/online-parser/build/site") {
        into("online-parser")
    }

    // Copy dokka output
    from(layout.buildDirectory.dir("dokka")) {
        into("kdoc")
    }

    // Copy generated docs social image from intermediate directory
    from(layout.buildDirectory.dir("socialImage")) {
        into("assets")
        include("social-image.png")
    }

    // Create index.md from README
    doLast {
        val readmeFile = file("README.md")
        val indexFile = outputDirectory.get().file("index.md").asFile

        if (readmeFile.exists()) {
            indexFile.writeText(
                """
                ---
                layout: default
                title: Home
                ---

                """.trimIndent() + readmeFile.readText()
            )
        }
    }
}

tasks.named("build") {
    dependsOn(bundleRelease)
}
