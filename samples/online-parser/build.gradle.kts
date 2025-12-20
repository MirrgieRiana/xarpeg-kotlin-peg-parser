import build_logic.generateSocialImage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven") }
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(false)
    outputColorName.set("RED")
}

group = "mirrg.xarpite.samples"
version = libs.versions.xarpeg.get()

kotlin {
    js(IR) {
        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_ES)
        }
        browser {
            commonWebpackConfig {
                outputFileName = "online-parser.js"
                mode = KotlinWebpackConfig.Mode.PRODUCTION
            }
        }
        binaries.library()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(libs.xarpeg)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val generateSocialImageTask by tasks.registering {
    group = "build"
    description = "Generates social image for documentation"

    val outputDir = layout.buildDirectory.dir("site/assets")
    val outputFile = outputDir.map { it.file("social-image.png").asFile }

    outputs.file(outputFile)

    doLast {
        generateSocialImage(
            outputFile = outputFile.get(),
            title = "Xarpeg",
            subtitle = "Kotlin PEG Parser"
        )
        println("Generated social image at ${outputFile.get().absolutePath}")
    }
}

val bundleRelease by tasks.registering(Sync::class) {
    group = "build"
    description = "Bundles the production JS output and resources into build/site."

    dependsOn("compileProductionLibraryKotlinJs", "jsProcessResources", "jsProductionLibraryCompileSync", generateSocialImageTask)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(layout.buildDirectory.dir("processedResources/js/main"))
    from(layout.buildDirectory.dir("js/packages/${project.name}/kotlin"))
    into(layout.buildDirectory.dir("site"))
}

tasks.named("build") {
    dependsOn(bundleRelease)
    dependsOn("ktlintFormat")
}

// Make ktlint check tasks run after format tasks
tasks.matching { it.name.startsWith("runKtlintCheck") }.configureEach {
    mustRunAfter(tasks.matching { it.name.startsWith("runKtlintFormat") })
}
