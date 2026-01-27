package io.github.mirrgieriana.xarpeg

/**
 * Indicates whether the current platform is Kotlin/Native.
 *
 * Used internally to optimize parser caching strategies for different platforms.
 */
expect val isNative: Boolean
