package io.github.mirrgieriana.xarpite.xarpeg

data class ParseResult<T>(val value: T, val rest: String)

typealias Parser<T> = (String) -> ParseResult<T>?

val parseA: Parser<String> = { input ->
    if (input.startsWith("a")) {
        ParseResult("a", input.drop(1))
    } else {
        null
    }
}

fun <T> repeatParser(parser: Parser<T>): Parser<List<T>> = { input ->
    var rest = input
    val results = mutableListOf<T>()

    while (true) {
        val result = parser(rest) ?: break
        val nextRest = result.rest
        // Avoid infinite loops when the parser succeeds without consuming input.
        if (nextRest == rest) break
        results.add(result.value)
        rest = nextRest
    }

    ParseResult(results, rest)
}
