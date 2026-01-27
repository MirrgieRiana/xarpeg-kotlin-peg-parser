import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.io.File
import build_logic.generateSocialImageWithPlaywright

plugins {
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("build-logic")
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

val bundleRelease = tasks.register<Sync>("bundleRelease") {
    group = "build"
    description = "Bundles the production JS output and resources into build/site."

    dependsOn("compileProductionLibraryKotlinJs", "jsProcessResources", "jsProductionLibraryCompileSync")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(layout.buildDirectory.dir("processedResources/js/main"))
    from(layout.buildDirectory.dir("js/packages/${project.name}/kotlin"))
    into(layout.buildDirectory.dir("site"))

    // Generate social image after sync completes
    doLast {
        val outputFile = layout.buildDirectory.dir("site/assets").get().file("social-image.png").asFile
        val sourceTemplate = project.file("src/jsMain/resources/social-image-template.html")

        // Create intermediate directory for social image generation
        val socialImageDir = layout.buildDirectory.dir("socialImage").get().asFile
        socialImageDir.mkdirs()

        // Copy HTML template to intermediate location
        val htmlTemplate = File(socialImageDir, "social-image-template.html")
        sourceTemplate.copyTo(htmlTemplate, overwrite = true)

        // Copy icon to intermediate location so HTML can reference it relatively
        val sourceIcon = project.file("../../assets/xarpeg-icon.svg")
        val targetIcon = File(socialImageDir, "xarpeg-icon.svg")
        if (sourceIcon.exists()) {
            sourceIcon.copyTo(targetIcon, overwrite = true)
        } else {
            throw RuntimeException("Icon file not found at ${sourceIcon.absolutePath}")
        }

        if (!htmlTemplate.exists()) {
            throw RuntimeException("HTML template not found at ${htmlTemplate.absolutePath}")
        }

        generateSocialImageWithPlaywright(
            htmlTemplate = htmlTemplate,
            outputFile = outputFile
        )
        println("Generated modern social image at ${outputFile.absolutePath}")
        println("Intermediate HTML template saved at ${htmlTemplate.absolutePath}")
    }
}

tasks.named("build") {
    dependsOn(bundleRelease)
    dependsOn("ktlintFormat")
}

// Make ktlint check tasks run after format tasks
tasks.matching { it.name.startsWith("runKtlintCheck") }.configureEach {
    mustRunAfter(tasks.matching { it.name.startsWith("runKtlintFormat") })
}
