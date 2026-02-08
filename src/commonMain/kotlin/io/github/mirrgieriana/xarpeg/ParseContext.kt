package io.github.mirrgieriana.xarpeg

class ParseContext(val src: String, val useMemoization: Boolean) {

    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    var isInNamedParser = false
    var isInLookAhead = false
    var errorPosition: Int = 0
    val suggestedParsers = mutableSetOf<Parser<*>>()

    private val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }
    fun toMatrixPosition(index: Int) = matrixPositionCalculator.toMatrixPosition(index)
    val errorMatrixPosition get() = toMatrixPosition(errorPosition)

    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        fun parse(): ParseResult<T>? {
            return if (!isInNamedParser && parser.name != null) {
                isInNamedParser = true
                val result = try {
                    parser.parseOrNull(this, start)
                } finally {
                    isInNamedParser = false
                }
                result
            } else {
                parser.parseOrNull(this, start)
            }
        }

        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                @Suppress("UNCHECKED_CAST")
                memo[key] as ParseResult<T>?
            } else {
                val result = parse()
                memo[key] = result
                result
            }
        } else {
            parse()
        }
        if (result == null && !isInNamedParser && !isInLookAhead && start >= errorPosition) {
            if (start > errorPosition) {
                errorPosition = start
                suggestedParsers.clear()
            }
            // Only add parsers with names to suggestions - unnamed parsers are just noise
            if (parser.name != null) {
                suggestedParsers += parser
            }
        }
        return result
    }

}

data class MatrixPosition(val row: Int, val column: Int)

class MatrixPositionCalculator(private val src: String) {
    private val lineStartIndices = run {
        val list = mutableListOf(0)
        src.forEachIndexed { index, char ->
            if (char == '\n') list.add(index + 1)
        }
        list
    }

    fun toMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }
}
