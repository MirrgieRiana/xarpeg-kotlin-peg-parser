package io.github.mirrgieriana.xarpeg

interface ParseContext {
    val src: String
    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>?
}

interface MemoizationParseContext {
    var useMemoization: Boolean
}

interface MatrixPositionCalculatorHolderParseContext {
    val matrixPositionCalculator: MatrixPositionCalculator
}

val ParseContext.matrixPositionCalculator get() = (this as? MatrixPositionCalculatorHolderParseContext)?.matrixPositionCalculator ?: MatrixPositionCalculator(src)

interface LookAheadHolderParseContext {
    var isInLookAhead: Boolean
}

interface SuggestingParseContext {
    val errorPosition: Int
    val suggestedParsers: Set<Parser<*>>
}

val ParseContext.errorPosition get() = (this as? SuggestingParseContext)?.errorPosition
val ParseContext.suggestedParsers: Set<Parser<*>> get() = (this as? SuggestingParseContext)?.suggestedParsers ?: emptySet()

fun ParseContext(src: String, useMemoization: Boolean = true): DefaultParseContext =
    DefaultParseContext(src).also { it.useMemoization = useMemoization }

open class DefaultParseContext(override val src: String) :
    ParseContext,
    MemoizationParseContext,
    MatrixPositionCalculatorHolderParseContext,
    LookAheadHolderParseContext,
    SuggestingParseContext {

    override var useMemoization: Boolean = true
    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    private var isInNamedParser = false
    override var isInLookAhead = false
    override var errorPosition: Int = 0
    override val suggestedParsers = mutableSetOf<Parser<*>>()

    override val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }

    override fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
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
            // Only add parsers with non-empty names to suggestions - unnamed and hidden parsers are just noise
            if (parser.name != null && parser.name != "") {
                suggestedParsers += parser
            }
        }
        return result
    }

}
