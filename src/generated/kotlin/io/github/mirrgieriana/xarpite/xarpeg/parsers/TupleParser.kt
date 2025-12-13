package io.github.mirrgieriana.xarpite.xarpeg.parsers

import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.Tuple0
import io.github.mirrgieriana.xarpite.xarpeg.Tuple1
import io.github.mirrgieriana.xarpite.xarpeg.Tuple2
import io.github.mirrgieriana.xarpite.xarpeg.Tuple3
import io.github.mirrgieriana.xarpite.xarpeg.Tuple4
import io.github.mirrgieriana.xarpite.xarpeg.Tuple5
import io.github.mirrgieriana.xarpite.xarpeg.Tuple6
import io.github.mirrgieriana.xarpite.xarpeg.Tuple7
import io.github.mirrgieriana.xarpite.xarpeg.Tuple8
import kotlin.jvm.JvmName

// Parser to Tuple1Parser

operator fun <T : Any> Parser<T>.unaryPlus(): Parser<Tuple1<T>> = this map { a -> Tuple1(a) }


// Parser Combination

/** パーサーの結合は純粋関数ではなく、位置にマッチしたり解析位置を進めたりする副作用があることに注意。 */
fun <L : Any, R : Any, T : Any> combine(left: Parser<L>, right: Parser<R>, function: (L, R) -> T) = Parser { context, start ->
    val resultL = context.parseOrNull(left, start) ?: return@Parser null
    val resultR = context.parseOrNull(right, resultL.end) ?: return@Parser null
    ParseResult(function(resultL.value, resultR.value), resultL.start, resultR.end)
}


// Tuple0Parser vs Tuple0Parser = Tuple0Parser

@JvmName("times00")
operator fun Parser<Tuple0>.times(other: Parser<Tuple0>) = combine(this, other) { _, _ -> Tuple0 }


// Tuple0Parser vs X = X

@JvmName("times0P")
operator fun <A : Any> Parser<Tuple0>.times(other: Parser<A>) = combine(this, other) { _, b -> b }

@JvmName("times01")
operator fun <A : Any> Parser<Tuple0>.times(other: Parser<Tuple1<A>>) = combine(this, other) { _, b -> b }

@JvmName("times02")
operator fun <A : Any, B : Any> Parser<Tuple0>.times(other: Parser<Tuple2<A, B>>) = combine(this, other) { _, b -> b }

@JvmName("times03")
operator fun <A : Any, B : Any, C : Any> Parser<Tuple0>.times(other: Parser<Tuple3<A, B, C>>) = combine(this, other) { _, b -> b }

@JvmName("times04")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple0>.times(other: Parser<Tuple4<A, B, C, D>>) = combine(this, other) { _, b -> b }

@JvmName("times05")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple0>.times(other: Parser<Tuple5<A, B, C, D, E>>) = combine(this, other) { _, b -> b }

@JvmName("times06")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple0>.times(other: Parser<Tuple6<A, B, C, D, E, F>>) = combine(this, other) { _, b -> b }

@JvmName("times07")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple0>.times(other: Parser<Tuple7<A, B, C, D, E, F, G>>) = combine(this, other) { _, b -> b }

@JvmName("times08")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple0>.times(other: Parser<Tuple8<A, B, C, D, E, F, G, H>>) = combine(this, other) { _, b -> b }


// X vs Tuple0Parser = X

@JvmName("timesP0")
operator fun <A : Any> Parser<A>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times10")
operator fun <A : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times20")
operator fun <A : Any, B : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times30")
operator fun <A : Any, B : Any, C : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times40")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times50")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple5<A, B, C, D, E>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times60")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple6<A, B, C, D, E, F>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times70")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple7<A, B, C, D, E, F, G>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }

@JvmName("times80")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple8<A, B, C, D, E, F, G, H>>.times(other: Parser<Tuple0>) = combine(this, other) { a, _ -> a }


// Parser vs Parser = Tuple2Parser

@JvmName("timesPP")
operator fun <A : Any, B : Any> Parser<A>.times(other: Parser<B>) = combine(this, other) { a, b -> Tuple2(a, b) }


// Parser vs TupleNParser = Tuple(N+1)Parser

@JvmName("timesP1")
operator fun <A : Any, B : Any> Parser<A>.times(other: Parser<Tuple1<B>>) = combine(this, other) { a, b -> Tuple2(a, b.a) }

@JvmName("timesP2")
operator fun <A : Any, B : Any, C : Any> Parser<A>.times(other: Parser<Tuple2<B, C>>) = combine(this, other) { a, b -> Tuple3(a, b.a, b.b) }

@JvmName("timesP3")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<A>.times(other: Parser<Tuple3<B, C, D>>) = combine(this, other) { a, b -> Tuple4(a, b.a, b.b, b.c) }

@JvmName("timesP4")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<A>.times(other: Parser<Tuple4<B, C, D, E>>) = combine(this, other) { a, b -> Tuple5(a, b.a, b.b, b.c, b.d) }

@JvmName("timesP5")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<A>.times(other: Parser<Tuple5<B, C, D, E, F>>) = combine(this, other) { a, b -> Tuple6(a, b.a, b.b, b.c, b.d, b.e) }

@JvmName("timesP6")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<A>.times(other: Parser<Tuple6<B, C, D, E, F, G>>) = combine(this, other) { a, b -> Tuple7(a, b.a, b.b, b.c, b.d, b.e, b.f) }

@JvmName("timesP7")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<A>.times(other: Parser<Tuple7<B, C, D, E, F, G, H>>) = combine(this, other) { a, b -> Tuple8(a, b.a, b.b, b.c, b.d, b.e, b.f, b.g) }


// TupleNParser vs Parser = Tuple(N+1)Parser

@JvmName("times1P")
operator fun <A : Any, B : Any> Parser<Tuple1<A>>.times(other: Parser<B>) = combine(this, other) { a, b -> Tuple2(a.a, b) }

@JvmName("times2P")
operator fun <A : Any, B : Any, C : Any> Parser<Tuple2<A, B>>.times(other: Parser<C>) = combine(this, other) { a, b -> Tuple3(a.a, a.b, b) }

@JvmName("times3P")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<D>) = combine(this, other) { a, b -> Tuple4(a.a, a.b, a.c, b) }

@JvmName("times4P")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<E>) = combine(this, other) { a, b -> Tuple5(a.a, a.b, a.c, a.d, b) }

@JvmName("times5P")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple5<A, B, C, D, E>>.times(other: Parser<F>) = combine(this, other) { a, b -> Tuple6(a.a, a.b, a.c, a.d, a.e, b) }

@JvmName("times6P")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple6<A, B, C, D, E, F>>.times(other: Parser<G>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, a.c, a.d, a.e, a.f, b) }

@JvmName("times7P")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple7<A, B, C, D, E, F, G>>.times(other: Parser<H>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, a.d, a.e, a.f, a.g, b) }


// TupleNParser vs TupleMParser = Tuple(N+M)Parser

@JvmName("times11")
operator fun <A : Any, B : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple1<B>>) = combine(this, other) { a, b -> Tuple2(a.a, b.a) }

@JvmName("times12")
operator fun <A : Any, B : Any, C : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple2<B, C>>) = combine(this, other) { a, b -> Tuple3(a.a, b.a, b.b) }

@JvmName("times13")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple3<B, C, D>>) = combine(this, other) { a, b -> Tuple4(a.a, b.a, b.b, b.c) }

@JvmName("times14")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple4<B, C, D, E>>) = combine(this, other) { a, b -> Tuple5(a.a, b.a, b.b, b.c, b.d) }

@JvmName("times15")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple5<B, C, D, E, F>>) = combine(this, other) { a, b -> Tuple6(a.a, b.a, b.b, b.c, b.d, b.e) }

@JvmName("times16")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple6<B, C, D, E, F, G>>) = combine(this, other) { a, b -> Tuple7(a.a, b.a, b.b, b.c, b.d, b.e, b.f) }

@JvmName("times17")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple1<A>>.times(other: Parser<Tuple7<B, C, D, E, F, G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, b.a, b.b, b.c, b.d, b.e, b.f, b.g) }

@JvmName("times21")
operator fun <A : Any, B : Any, C : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple1<C>>) = combine(this, other) { a, b -> Tuple3(a.a, a.b, b.a) }

@JvmName("times22")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple2<C, D>>) = combine(this, other) { a, b -> Tuple4(a.a, a.b, b.a, b.b) }

@JvmName("times23")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple3<C, D, E>>) = combine(this, other) { a, b -> Tuple5(a.a, a.b, b.a, b.b, b.c) }

@JvmName("times24")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple4<C, D, E, F>>) = combine(this, other) { a, b -> Tuple6(a.a, a.b, b.a, b.b, b.c, b.d) }

@JvmName("times25")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple5<C, D, E, F, G>>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, b.a, b.b, b.c, b.d, b.e) }

@JvmName("times26")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple2<A, B>>.times(other: Parser<Tuple6<C, D, E, F, G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, b.a, b.b, b.c, b.d, b.e, b.f) }

@JvmName("times31")
operator fun <A : Any, B : Any, C : Any, D : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple1<D>>) = combine(this, other) { a, b -> Tuple4(a.a, a.b, a.c, b.a) }

@JvmName("times32")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple2<D, E>>) = combine(this, other) { a, b -> Tuple5(a.a, a.b, a.c, b.a, b.b) }

@JvmName("times33")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple3<D, E, F>>) = combine(this, other) { a, b -> Tuple6(a.a, a.b, a.c, b.a, b.b, b.c) }

@JvmName("times34")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple4<D, E, F, G>>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, a.c, b.a, b.b, b.c, b.d) }

@JvmName("times35")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple3<A, B, C>>.times(other: Parser<Tuple5<D, E, F, G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, b.a, b.b, b.c, b.d, b.e) }

@JvmName("times41")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<Tuple1<E>>) = combine(this, other) { a, b -> Tuple5(a.a, a.b, a.c, a.d, b.a) }

@JvmName("times42")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<Tuple2<E, F>>) = combine(this, other) { a, b -> Tuple6(a.a, a.b, a.c, a.d, b.a, b.b) }

@JvmName("times43")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<Tuple3<E, F, G>>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, a.c, a.d, b.a, b.b, b.c) }

@JvmName("times44")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple4<A, B, C, D>>.times(other: Parser<Tuple4<E, F, G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, a.d, b.a, b.b, b.c, b.d) }

@JvmName("times51")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Parser<Tuple5<A, B, C, D, E>>.times(other: Parser<Tuple1<F>>) = combine(this, other) { a, b -> Tuple6(a.a, a.b, a.c, a.d, a.e, b.a) }

@JvmName("times52")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple5<A, B, C, D, E>>.times(other: Parser<Tuple2<F, G>>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, a.c, a.d, a.e, b.a, b.b) }

@JvmName("times53")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple5<A, B, C, D, E>>.times(other: Parser<Tuple3<F, G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, a.d, a.e, b.a, b.b, b.c) }

@JvmName("times61")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Parser<Tuple6<A, B, C, D, E, F>>.times(other: Parser<Tuple1<G>>) = combine(this, other) { a, b -> Tuple7(a.a, a.b, a.c, a.d, a.e, a.f, b.a) }

@JvmName("times62")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple6<A, B, C, D, E, F>>.times(other: Parser<Tuple2<G, H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, a.d, a.e, a.f, b.a, b.b) }

@JvmName("times71")
operator fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any> Parser<Tuple7<A, B, C, D, E, F, G>>.times(other: Parser<Tuple1<H>>) = combine(this, other) { a, b -> Tuple8(a.a, a.b, a.c, a.d, a.e, a.f, a.g, b.a) }
