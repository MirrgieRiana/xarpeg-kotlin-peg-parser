package mirrg.xarpite.peg

data class ParseResult<T>(val value: T, val rest: String)

typealias Parser<T> = (String) -> ParseResult<T>?

val parseA: Parser<String> = { input ->
    if (input.startsWith("a")) {
        ParseResult("a", input.drop(1))
    } else {
        null
    }
}

fun <T> repeatParser(parser: Parser<T>): (String) -> ParseResult<List<T>> = fun(input: String): ParseResult<List<T>> {
    var rest = input
    val results = mutableListOf<T>()

    while (true) {
        val result = parser(rest) ?: break
        if (result.rest.length >= rest.length) break
        results.add(result.value)
        rest = result.rest
    }

    return ParseResult(results, rest)
}
