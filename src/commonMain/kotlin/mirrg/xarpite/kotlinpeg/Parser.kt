package mirrg.xarpite.kotlinpeg

import kotlin.math.max
import kotlin.math.min

/**
 * Token produced by primitive parsers such as [text] and [regex].
 */
data class Token(val text: String, val start: Int, val end: Int)

data class ParseConfig(val memoize: Boolean = true)

sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(
        val position: Int,
        val expected: Set<String>,
        val found: String,
        val diagnostic: String,
    ) : ParseResult<Nothing>()
}

sealed class StepResult<out T> {
    data class Ok<T>(val value: T, val next: Int) : StepResult<T>()
    data class Err(val position: Int, val expected: Set<String>) : StepResult<Nothing>()
}

private fun StepResult.Err.merge(other: StepResult.Err): StepResult.Err {
    return when {
        other.position > position -> other
        other.position < position -> this
        else -> StepResult.Err(position, expected + other.expected)
    }
}

class Context(val input: String, val config: ParseConfig) {
    private val memo = mutableMapOf<Pair<Int, Parser<*>>, StepResult<*>>()

    fun <T> memoized(parser: Parser<T>, pos: Int, compute: () -> StepResult<T>): StepResult<T> {
        if (!config.memoize) return compute()
        val key = pos to parser
        @Suppress("UNCHECKED_CAST")
        return memo.getOrPut(key) { compute() } as StepResult<T>
    }
}

interface Parser<T> {
    fun parse(ctx: Context, pos: Int): StepResult<T>
}

class Grammar<T>(private val start: Parser<T>) {
    fun parse(input: String, config: ParseConfig = ParseConfig()): T {
        return when (val result = tryParse(input, config)) {
            is ParseResult.Success -> result.value
            is ParseResult.Failure -> error(result.diagnostic)
        }
    }

    fun tryParse(input: String, config: ParseConfig = ParseConfig()): ParseResult<T> {
        val ctx = Context(input, config)
        val first = start.parse(ctx, 0)
        return when (first) {
            is StepResult.Ok -> {
                if (first.next == input.length) {
                    ParseResult.Success(first.value)
                } else {
                    val failure = StepResult.Err(first.next, setOf("end of input"))
                    failure.toFailure(input)
                }
            }

            is StepResult.Err -> first.toFailure(input)
        }
    }
}

class PegBuilder {
    private var startParser: Parser<Any?>? = null

    fun <T> start(parser: Parser<T>) {
        @Suppress("UNCHECKED_CAST")
        startParser = parser as Parser<Any?>
    }

    internal fun <T> build(): Grammar<T> {
        val parser = startParser ?: error("start parser is not defined")
        @Suppress("UNCHECKED_CAST")
        return Grammar(parser as Parser<T>)
    }
}

fun <T> peg(block: PegBuilder.() -> Unit): Grammar<T> {
    val builder = PegBuilder()
    builder.block()
    return builder.build()
}

class Rule<T>(private val name: String) : Parser<T> {
    private var inner: Parser<T>? = null

    fun define(parser: Parser<T>) {
        inner = parser
    }

    override fun parse(ctx: Context, pos: Int): StepResult<T> {
        val parser = inner ?: error("Rule '$name' is not defined")
        return ctx.memoized(this, pos) { parser.parse(ctx, pos) }
    }

    override fun toString(): String = name
}

fun <T> rule(name: String): Rule<T> = Rule(name)

fun text(content: String): Parser<Token> = object : Parser<Token> {
    override fun parse(ctx: Context, pos: Int): StepResult<Token> {
        return ctx.memoized(this, pos) {
            if (ctx.input.startsWith(content, pos)) {
                val end = pos + content.length
                StepResult.Ok(Token(content, pos, end), end)
            } else {
                StepResult.Err(pos, setOf("'$content'"))
            }
        }
    }

    override fun toString(): String = "'$content'"
}

fun regex(pattern: String): Parser<Token> {
    val regex = pattern.toRegex()
    return object : Parser<Token> {
        override fun parse(ctx: Context, pos: Int): StepResult<Token> {
            return ctx.memoized(this, pos) {
                val match = regex.find(ctx.input, pos)
                if (match != null && match.range.first == pos) {
                    val value = match.value
                    StepResult.Ok(Token(value, pos, pos + value.length), pos + value.length)
                } else {
                    StepResult.Err(pos, setOf("regex($pattern)"))
                }
            }
        }

        override fun toString(): String = "regex($pattern)"
    }
}

fun <T, R> Parser<T>.map(transform: (T) -> R): Parser<R> = object : Parser<R> {
    override fun parse(ctx: Context, pos: Int): StepResult<R> {
        return ctx.memoized(this, pos) {
            when (val res = this@map.parse(ctx, pos)) {
                is StepResult.Ok -> StepResult.Ok(transform(res.value), res.next)
                is StepResult.Err -> res
            }
        }
    }
}

fun <T> choice(vararg parsers: Parser<out T>): Parser<T> = object : Parser<T> {
    override fun parse(ctx: Context, pos: Int): StepResult<T> {
        return ctx.memoized(this, pos) {
            var failure = StepResult.Err(pos, emptySet())
            for (parser in parsers) {
                @Suppress("UNCHECKED_CAST")
                val typed = parser as Parser<T>
                val result = ctx.memoized(typed, pos) { typed.parse(ctx, pos) }
                when (result) {
                    is StepResult.Ok -> return@memoized result
                    is StepResult.Err -> failure = failure.merge(result)
                }
            }
            failure
        }
    }
}

fun seq(vararg parsers: Parser<*>): Parser<List<Any?>> = object : Parser<List<Any?>> {
    override fun parse(ctx: Context, pos: Int): StepResult<List<Any?>> {
        return ctx.memoized(this, pos) {
            var position = pos
            val values = mutableListOf<Any?>()
            for (parser in parsers) {
                val result = parser.parse(ctx, position)
                when (result) {
                    is StepResult.Ok -> {
                        values += result.value
                        position = result.next
                    }

                    is StepResult.Err -> return@memoized result
                }
            }
            StepResult.Ok(values, position)
        }
    }
}

fun <T> optional(parser: Parser<T>): Parser<T?> = object : Parser<T?> {
    override fun parse(ctx: Context, pos: Int): StepResult<T?> {
        return ctx.memoized(this, pos) {
            when (val res = parser.parse(ctx, pos)) {
                is StepResult.Ok -> StepResult.Ok(res.value, res.next)
                is StepResult.Err -> StepResult.Ok(null, pos)
            }
        }
    }
}

fun <T> Parser<T>.many(): Parser<List<T>> = object : Parser<List<T>> {
    override fun parse(ctx: Context, pos: Int): StepResult<List<T>> {
        return ctx.memoized(this, pos) {
            var position = pos
            val values = mutableListOf<T>()
            while (true) {
                val result = this@many.parse(ctx, position)
                when (result) {
                    is StepResult.Ok -> {
                        if (result.next == position) return@memoized StepResult.Ok(values, position)
                        values += result.value
                        position = result.next
                    }

                    is StepResult.Err -> return@memoized StepResult.Ok(values, position)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            StepResult.Ok(values, position)
        }
    }
}

fun <T> Parser<T>.many1(): Parser<List<T>> = object : Parser<List<T>> {
    override fun parse(ctx: Context, pos: Int): StepResult<List<T>> {
        return ctx.memoized(this, pos) {
            val first = this@many1.parse(ctx, pos)
            when (first) {
                is StepResult.Err -> first
                is StepResult.Ok -> {
                    val rest = this@many1.many().parse(ctx, first.next)
                    when (rest) {
                        is StepResult.Ok -> StepResult.Ok(listOf(first.value) + rest.value, rest.next)
                        is StepResult.Err -> rest
                    }
                }
            }
        }
    }
}

fun <T> not(parser: Parser<T>): Parser<Unit> = object : Parser<Unit> {
    override fun parse(ctx: Context, pos: Int): StepResult<Unit> {
        return ctx.memoized(this, pos) {
            when (val res = parser.parse(ctx, pos)) {
                is StepResult.Ok -> StepResult.Err(pos, setOf("not ${parser}"))
                is StepResult.Err -> StepResult.Ok(Unit, pos)
            }
        }
    }
}

fun <T> leftAssoc(
    term: Parser<T>,
    op: Parser<*>,
    combine: (T, Token, T) -> T,
): Parser<T> = object : Parser<T> {
    override fun parse(ctx: Context, pos: Int): StepResult<T> {
        return ctx.memoized(this, pos) {
            val first = term.parse(ctx, pos)
            if (first is StepResult.Err) return@memoized first
            first as StepResult.Ok

            var acc = first.value
            var position = first.next
            while (true) {
                when (val opRes = op.parse(ctx, position)) {
                    is StepResult.Err -> return@memoized StepResult.Ok(acc, position)
                    is StepResult.Ok -> {
                        val right = term.parse(ctx, opRes.next)
                        when (right) {
                            is StepResult.Err -> return@memoized right
                            is StepResult.Ok -> {
                                val operator = (opRes.value as? Token)
                                    ?: Token(opRes.value.toString(), position, opRes.next)
                                acc = combine(acc, operator, right.value)
                                position = right.next
                            }
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            StepResult.Ok(acc, position)
        }
    }
}

private fun StepResult.Err.toFailure(input: String): ParseResult.Failure {
    val found = if (position < input.length) {
        input.substring(position, min(position + 10, input.length)).replace("\n", "\\n")
    } else {
        "end of input"
    }
    val diag = buildString {
        append("Parse error at position ")
        append(position)
        append(": expected ")
        append(expected.ifEmpty { setOf("token") }.joinToString(" or "))
        append(" but found '")
        append(found)
        append('\'')
        append("\n")
        val start = max(0, position - 10)
        val end = min(input.length, position + 10)
        append(input.substring(start, end).replace("\n", "\\n"))
        append("\n")
        append(" ".repeat(position - start))
        append("^")
    }
    return ParseResult.Failure(position, expected, found, diag)
}
