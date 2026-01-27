package io.github.mirrgieriana.xarpeg

/**
 * Context for parsing operations.
 *
 * Maintains parsing state including memoization cache, error tracking, and position information.
 *
 * @param src The input string being parsed.
 * @param useMemoization Whether to cache parsing results for better performance with backtracking.
 */
class ParseContext(val src: String, val useMemoization: Boolean) {

    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    /**
     * Whether currently inside a named parser.
     *
     * This flag prevents nested named parsers from interfering with error tracking.
     */
    var isInNamedParser = false
    
    /**
     * The furthest position where parsing has failed.
     *
     * Used to provide meaningful error messages by tracking the deepest point reached in the input.
     */
    var errorPosition: Int = 0
    
    /**
     * Set of parsers that could have matched at [errorPosition].
     *
     * Used to generate helpful error messages showing what was expected at the failure point.
     */
    val suggestedParsers = mutableSetOf<Parser<*>>()

    private val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }
    
    /**
     * Converts a linear position to a (row, column) position.
     *
     * @param index The position in the input string.
     * @return A [MatrixPosition] with row and column numbers (1-indexed).
     */
    fun toMatrixPosition(index: Int) = matrixPositionCalculator.toMatrixPosition(index)
    
    /**
     * The (row, column) position of the parsing error.
     */
    val errorMatrixPosition get() = toMatrixPosition(errorPosition)

    /**
     * Attempts to parse using the given parser at the specified position.
     *
     * This method handles memoization and error tracking automatically.
     *
     * @param parser The parser to use.
     * @param start The starting position in the input.
     * @return A [ParseResult] if parsing succeeds, or `null` if it fails.
     */
    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                @Suppress("UNCHECKED_CAST")
                memo[key] as ParseResult<T>?
            } else {
                val result = if (!isInNamedParser && parser.name != null) {
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
                memo[key] = result
                result
            }
        } else {
            if (!isInNamedParser && parser.name != null) {
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
        if (result == null && !isInNamedParser && start >= errorPosition) {
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

/**
 * Represents a position in the input as (row, column).
 *
 * Both row and column are 1-indexed.
 *
 * @param row The line number (1-indexed).
 * @param column The column number within the line (1-indexed).
 */
data class MatrixPosition(val row: Int, val column: Int)

/**
 * Converts linear string positions to (row, column) positions.
 *
 * Efficiently tracks line breaks to enable quick conversion from string indices to human-readable positions.
 *
 * @param src The input string to analyze.
 */
class MatrixPositionCalculator(private val src: String) {
    private val lineStartIndices = run {
        val list = mutableListOf(0)
        src.forEachIndexed { index, char ->
            if (char == '\n') list.add(index + 1)
        }
        list
    }

    /**
     * Converts a linear position to (row, column) format.
     *
     * @param index The position in the source string (0-indexed).
     * @return A [MatrixPosition] with 1-indexed row and column numbers.
     * @throws IllegalArgumentException if the index is out of range.
     */
    fun toMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }
}
