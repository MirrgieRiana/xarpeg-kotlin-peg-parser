package io.github.mirrgieriana.xarpite.xarpeg

/**
 * A simple hello world function for demonstration purposes.
 */
fun helloWorld(): String {
    return "Hello, World"
}

private fun runMain(args: Array<String>) {
    println(helloWorld())
    if (args.isNotEmpty()) {
        println("Args: ${args.joinToString(", ")}")
    }
}

/**
 * Main entry point for the sample application.
 */
fun main(args: Array<String>) = runMain(args)

/**
 * Zero-argument entry point used by native/JS executables.
 */
fun main() = runMain(emptyArray())
