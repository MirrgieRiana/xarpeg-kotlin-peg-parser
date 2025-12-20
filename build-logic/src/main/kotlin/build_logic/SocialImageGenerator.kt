package build_logic

/**
 * Social image generation utilities.
 * 
 * Note: The actual implementation is in samples/online-parser/build.gradle.kts
 * because it requires Playwright dependency which is specific to that project.
 * 
 * This file serves as documentation for the social image generation approach:
 * 
 * 1. HTML Template: A fixed-size HTML (1200x630px) is created with modern design
 * 2. Playwright Rendering: The HTML is rendered using Playwright Chromium browser
 * 3. Screenshot: A high-resolution PNG (2400x1260px @ 2x DPI) is captured
 * 4. Safe Zones: Important content is kept within recommended safe zones
 * 
 * The generated image includes:
 * - Modern gradient background (purple to blue)
 * - Decorative geometric shapes
 * - Typography with proper hierarchy
 * - Glassmorphism-style badge
 * - Proper contrast and accessibility
 */

