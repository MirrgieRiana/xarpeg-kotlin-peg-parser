import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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

/**
 * Generates a social image (Open Graph / Twitter Card) for documentation pages.
 * Standard size: 1200x630 pixels
 */
fun generateSocialImage(
    outputFile: File,
    title: String = "Xarpeg",
    subtitle: String = "Kotlin PEG Parser",
    backgroundColor: Color = Color(0x15, 0x9A, 0x57), // GitHub green similar to Cayman theme
    textColor: Color = Color.WHITE,
) {
    val width = 1200
    val height = 630

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()

    // Enable anti-aliasing for better text quality
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    // Fill background
    g.color = backgroundColor
    g.fillRect(0, 0, width, height)

    // Draw title
    g.color = textColor
    g.font = Font("SansSerif", Font.BOLD, 120)
    val titleMetrics = g.fontMetrics
    val titleWidth = titleMetrics.stringWidth(title)
    val titleX = (width - titleWidth) / 2
    val titleY = height / 2 - 40
    g.drawString(title, titleX, titleY)

    // Draw subtitle
    g.font = Font("SansSerif", Font.PLAIN, 60)
    val subtitleMetrics = g.fontMetrics
    val subtitleWidth = subtitleMetrics.stringWidth(subtitle)
    val subtitleX = (width - subtitleWidth) / 2
    val subtitleY = height / 2 + 60
    g.drawString(subtitle, subtitleX, subtitleY)

    g.dispose()

    // Ensure output directory exists
    outputFile.parentFile?.mkdirs()

    // Write PNG file
    ImageIO.write(image, "png", outputFile)
}

val bundleRelease by tasks.registering(Sync::class) {
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
        generateSocialImage(
            outputFile = outputFile,
            title = "Xarpeg",
            subtitle = "Kotlin PEG Parser"
        )
        println("Generated social image at ${outputFile.absolutePath}")
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
