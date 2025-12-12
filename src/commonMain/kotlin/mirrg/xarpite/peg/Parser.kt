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

fun <T> repeatParser(parser: Parser<T>): Parser<List<T>> = fun(input: String): ParseResult<List<T>>? {
    var rest = input
    val results = mutableListOf<T>()

    while (true) {
        val result = parser(rest) ?: break
        results.add(result.value)
        val nextRest = result.rest
        if (nextRest.length == rest.length) break
        rest = nextRest
    }

    return ParseResult(results, rest)
}
