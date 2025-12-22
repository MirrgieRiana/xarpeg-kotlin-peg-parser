package build_logic

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import java.io.File
import java.nio.file.Paths

/**
 * Generates a social image using Playwright to render HTML template.
 * Creates a modern design with proper safe zones for social media.
 * Standard size: 1200x630px (recommended for Twitter, Discord, etc.)
 */
fun generateSocialImageWithPlaywright(
    htmlTemplate: File,
    outputFile: File
) {
    val width = 1200
    val height = 630

    // Ensure output directory exists
    outputFile.parentFile?.mkdirs()

    try {
        Playwright.create().use { playwright ->
            val browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(
                        listOf(
                            "--disable-background-networking",
                            "--disable-component-extensions-with-background-pages",
                            "--disable-component-update"
                        )
                    )
            )

            val context = browser.newContext(
                Browser.NewContextOptions()
                    .setViewportSize(width, height)
                    .setDeviceScaleFactor(1.0)  // Standard resolution, not 2x
            )

            val page = context.newPage()
            page.navigate("file://${htmlTemplate.absolutePath}")

            // Wait for all resources to load
            page.waitForLoadState(LoadState.NETWORKIDLE)

            // Take screenshot
            page.screenshot(
                Page.ScreenshotOptions()
                    .setPath(Paths.get(outputFile.absolutePath))
                    .setFullPage(false)
                    .setType(ScreenshotType.PNG)
            )

            browser.close()
            println("Screenshot saved to: ${outputFile.absolutePath}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException("Failed to generate social image: ${e.message}", e)
    }
}
