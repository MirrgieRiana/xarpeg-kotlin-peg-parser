package build_logic

fun getTupleSrc(maxElementCount: Int): String {
    val typeParams = (0 until maxElementCount).map { index -> ('A'.code + index).toChar().toString() }
    return buildString {
        appendLine("package io.github.mirrgieriana.xarpeg")
        appendLine()
        appendLine("object Tuple0")
        (1..maxElementCount).forEach { n ->
            val params = typeParams.take(n)
            val typeParamStr = params.joinToString(", ") { "out $it" }
            val paramStr = params.mapIndexed { _, param -> "val ${param.lowercase()}: $param" }.joinToString(", ")
            appendLine("data class Tuple$n<$typeParamStr>($paramStr)")
        }
    }
}

fun getTupleParserSrc(maxElementCount: Int): String {
    val typeParams = (0 until maxElementCount).map { index -> ('A'.code + index).toChar().toString() }
    return buildString {
        appendLine("package io.github.mirrgieriana.xarpeg.parsers")
        appendLine()
        appendLine("import io.github.mirrgieriana.xarpeg.ParseResult")
        appendLine("import io.github.mirrgieriana.xarpeg.Parser")
        appendLine("import io.github.mirrgieriana.xarpeg.Tuple0")
        (1..maxElementCount).forEach { n ->
            appendLine("import io.github.mirrgieriana.xarpeg.Tuple$n")
        }
        appendLine("import kotlin.jvm.JvmName")
        appendLine()
        appendLine("// Parser to Tuple1Parser")
        appendLine()
        appendLine("operator fun <T : Any> Parser<T>.unaryPlus(): Parser<Tuple1<T>> = this map { a -> Tuple1(a) }")
        appendLine()
        appendLine()
        appendLine("// Parser Combination")
        appendLine()
        appendLine("/** パーサーの結合は純粋関数ではなく、位置にマッチしたり解析位置を進めたりする副作用があることに注意。 */")
        appendLine("fun <L : Any, R : Any, T : Any> combine(left: Parser<L>, right: Parser<R>, function: (L, R) -> T) = Parser { context, start ->")
        appendLine("    val resultL = context.parseOrNull(left, start) ?: return@Parser null")
        appendLine("    val resultR = context.parseOrNull(right, resultL.end) ?: return@Parser null")
        appendLine("    ParseResult(function(resultL.value, resultR.value), resultL.start, resultR.end)")
        appendLine("}")
        appendLine()
        appendLine()
        appendLine("// Tuple0Parser vs Tuple0Parser = Tuple0Parser")
        appendLine()
        appendLine("@JvmName(\"times00\")")
        appendLine("operator fun Parser<Tuple0>.times(other: Parser<Tuple0>) = combine(this, other) { _, _ -> Tuple0 }")
        appendLine()
        appendLine()
        appendLine("// Tuple0Parser vs X = X")
        appendLine()
        appendLine("@JvmName(\"times0P\")")
        appendLine("operator fun <A : Any> Parser<Tuple0>.times(other: Parser<A>) = combine(this, other) { _, b -> b }")
        appendLine()
        (1..maxElementCount).forEach { n ->
            val params = typeParams.take(n)
            val typeParamStr = params.joinToString(", ")
            appendLine("@JvmName(\"times0$n\")")
            appendLine("operator fun <$typeParamStr> Parser<Tuple0>.times(other: Parser<Tuple$n<${params.joinToString(", ")}>>) = combine(this, other) { _, b -> b }")
            appendLine()
        }
        appendLine()
        appendLine("// X vs Tuple0Parser = X")
        appendLine()
        appendLine("@JvmName(\"timesP0\")")
        appendLine("operator fun <A : Any> Parser<A>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }")
        appendLine()
        (1..maxElementCount).forEach { n ->
            val params = typeParams.take(n)
            val typeParamStr = params.joinToString(", ")
            appendLine("@JvmName(\"times${n}0\")")
            appendLine("operator fun <$typeParamStr> Parser<Tuple$n<${params.joinToString(", ")}>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }")
            appendLine()
        }
        appendLine()
        appendLine("// Parser vs Parser = Tuple2Parser")
        appendLine()
        appendLine("@JvmName(\"timesPP\")")
        appendLine("operator fun <A : Any, B : Any> Parser<A>.times(other: Parser<B>) = combine(this, other) { a, b -> Tuple2(a, b) }")
        appendLine()
        appendLine()
        appendLine("// Parser vs TupleNParser = Tuple(N+1)Parser")
        appendLine()
        (1..(maxElementCount - 1)).forEach { n ->
            val resultN = n + 1
            val rightParams = typeParams.subList(1, n + 1) // skip A
            val resultParams = typeParams.take(resultN)
            val typeParamStr = "A : Any" + if (rightParams.isNotEmpty()) ", " + rightParams.joinToString(", ") else ""
            val rightTupleAccess = (0 until n).map { i -> "b.${typeParams[i].lowercase()}" }.joinToString(", ")
            appendLine("@JvmName(\"timesP$n\")")
            appendLine("operator fun <$typeParamStr> Parser<A>.times(other: Parser<Tuple$n<${rightParams.joinToString(", ")}>>) = combine(this, other) { a, b -> Tuple$resultN(a, $rightTupleAccess) }")
            appendLine()
        }
        appendLine()
        appendLine("// TupleNParser vs Parser = Tuple(N+1)Parser")
        appendLine()
        (1..(maxElementCount - 1)).forEach { n ->
            val resultN = n + 1
            val leftParams = typeParams.take(n)
            val resultParams = typeParams.take(resultN)
            val newParam = typeParams[n]
            val typeParamStr = (leftParams + newParam).joinToString(", ") { if (it == newParam) "$it : Any" else it }
            val leftTupleAccess = leftParams.mapIndexed { i, _ -> "a.${typeParams[i].lowercase()}" }.joinToString(", ")
            appendLine("@JvmName(\"times${n}P\")")
            appendLine("operator fun <$typeParamStr> Parser<Tuple$n<${leftParams.joinToString(", ")}>>.times(other: Parser<$newParam>) = combine(this, other) { a, b -> Tuple$resultN($leftTupleAccess, b) }")
            appendLine()
        }
        appendLine()
        appendLine("// TupleNParser vs TupleMParser = Tuple(N+M)Parser")
        appendLine()
        val combinations = mutableListOf<Triple<Int, Int, Int>>()
        (1..(maxElementCount - 1)).forEach { leftN ->
            (1..(maxElementCount - 1)).forEach { rightN ->
                val resultN = leftN + rightN
                if (resultN <= maxElementCount) {
                    combinations.add(Triple(leftN, rightN, resultN))
                }
            }
        }
        combinations.forEachIndexed { index, (leftN, rightN, resultN) ->
            val leftParams = typeParams.take(leftN)
            val rightParams = typeParams.subList(leftN, leftN + rightN)
            val resultParams = typeParams.take(resultN)
            val typeParamStr = resultParams.joinToString(", ")
            val leftTupleAccess = leftParams.mapIndexed { i, _ -> "a.${typeParams[i].lowercase()}" }.joinToString(", ")
            val rightTupleAccess = rightParams.mapIndexed { i, _ -> "b.${typeParams[i].lowercase()}" }.joinToString(", ")
            appendLine("@JvmName(\"times${leftN}_${rightN}\")")
            append("operator fun <$typeParamStr> Parser<Tuple$leftN<${leftParams.joinToString(", ")}>>.times(other: Parser<Tuple$rightN<${rightParams.joinToString(", ")}>>) = combine(this, other) { a, b -> Tuple$resultN($leftTupleAccess, $rightTupleAccess) }")
            if (index < combinations.size - 1) {
                appendLine()
                appendLine()
            } else {
                appendLine()
            }
        }
    }
}
