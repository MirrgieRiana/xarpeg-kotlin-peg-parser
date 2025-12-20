import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.io.File

plugins {
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven") }
}

// Playwright dependency for HTML to PNG conversion
configurations {
    create("playwright")
}

dependencies {
    "playwright"("com.microsoft.playwright:playwright:1.41.0")
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
 * Generates a social image using Playwright to render HTML template.
 * Creates a modern, gradient-based design with proper safe zones.
 */
fun generateSocialImageWithPlaywright(
    htmlTemplate: File,
    outputFile: File,
    playwrightClasspath: FileCollection
) {
    val width = 1200
    val height = 630

    // Create a temporary directory for our Java source
    val tempDir = File(System.getProperty("java.io.tmpdir"), "playwright-screenshot-${System.currentTimeMillis()}")
    tempDir.mkdirs()

    val scriptFile = File(tempDir, "PlaywrightScreenshot.java")
    scriptFile.writeText("""
        import com.microsoft.playwright.*;
        import com.microsoft.playwright.options.*;
        import java.nio.file.Paths;
        
        public class PlaywrightScreenshot {
            public static void main(String[] args) {
                try (Playwright playwright = Playwright.create()) {
                    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(java.util.Arrays.asList(
                            "--disable-background-networking",
                            "--disable-component-extensions-with-background-pages",
                            "--disable-component-update"
                        ))
                    );
                    
                    BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setViewportSize(1200, 630)
                        .setDeviceScaleFactor(2.0)
                    );
                    
                    Page page = context.newPage();
                    page.navigate("file://" + args[0]);
                    
                    // Wait for all resources to load
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    
                    // Take screenshot
                    page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(args[1]))
                        .setFullPage(false)
                        .setType(ScreenshotType.PNG)
                    );
                    
                    browser.close();
                    System.out.println("Screenshot saved to: " + args[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    """.trimIndent())

    // Ensure output directory exists
    outputFile.parentFile?.mkdirs()

    // Compile and run the Playwright script
    val javaHome = System.getProperty("java.home")
    val javac = "$javaHome/bin/javac"
    val java = "$javaHome/bin/java"

    // Compile
    val compileProcess = ProcessBuilder(
        javac,
        "-cp", playwrightClasspath.asPath,
        "-d", tempDir.absolutePath,
        scriptFile.absolutePath
    ).redirectErrorStream(true).start()

    val compileOutput = compileProcess.inputStream.bufferedReader().readText()
    compileProcess.waitFor()

    if (compileProcess.exitValue() != 0) {
        tempDir.deleteRecursively()
        throw RuntimeException("Failed to compile Playwright script:\n$compileOutput")
    }

    // Run
    val runProcess = ProcessBuilder(
        java,
        "-cp", "${tempDir.absolutePath}${File.pathSeparator}${playwrightClasspath.asPath}",
        "PlaywrightScreenshot",
        htmlTemplate.absolutePath,
        outputFile.absolutePath
    ).redirectErrorStream(true).start()

    val runOutput = runProcess.inputStream.bufferedReader().readText()
    runProcess.waitFor()

    // Cleanup
    tempDir.deleteRecursively()

    if (runProcess.exitValue() != 0) {
        throw RuntimeException("Failed to generate screenshot:\n$runOutput")
    }

    println(runOutput)
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
        val sourceTemplate = project.file("src/jsMain/resources/social-image-template.html")

        // Create intermediate directory for social image generation
        val socialImageDir = layout.buildDirectory.dir("socialImage").get().asFile
        socialImageDir.mkdirs()

        // Copy HTML template to intermediate location
        val htmlTemplate = File(socialImageDir, "social-image-template.html")
        sourceTemplate.copyTo(htmlTemplate, overwrite = true)

        if (!htmlTemplate.exists()) {
            throw RuntimeException("HTML template not found at ${htmlTemplate.absolutePath}")
        }

        val playwrightClasspath = configurations.getByName("playwright")

        generateSocialImageWithPlaywright(
            htmlTemplate = htmlTemplate,
            outputFile = outputFile,
            playwrightClasspath = playwrightClasspath
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
