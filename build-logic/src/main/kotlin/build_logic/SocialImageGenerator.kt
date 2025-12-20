package build_logic

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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
