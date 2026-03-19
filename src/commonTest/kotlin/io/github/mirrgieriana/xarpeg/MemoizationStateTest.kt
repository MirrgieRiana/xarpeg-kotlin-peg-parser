package io.github.mirrgieriana.xarpeg

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for state-dependent memoization via [DefaultParseContext.getState].
 *
 * When getState() returns different values, the memoization table is partitioned by state
 * so that cached results from one state are never served for another.
 */
class MemoizationStateTest {

    private class StatefulParseContext(src: String) : DefaultParseContext(src) {
        var currentState: Any = Unit
        override fun getState(): Any = currentState
    }

    @Test
    fun defaultGetStateUsesSingleMemoTable() {
        // デフォルトのgetState()はUnitを返すため、メモテーブルは1つだけ使われる
        var callCount = 0
        val parser = Parser<Int> { _, start ->
            callCount++
            ParseResult(callCount, start, start)
        }

        val context = DefaultParseContext("test")

        val result1 = context.parseOrNull(parser, 0)
        assertEquals(1, result1?.value)
        assertEquals(1, callCount)

        // 同じ位置での再呼び出しはキャッシュから返される
        val result2 = context.parseOrNull(parser, 0)
        assertEquals(1, result2?.value)
        assertEquals(1, callCount) // パーサーは再評価されない
    }

    @Test
    fun differentStatesUseSeparateMemoTables() {
        // getState()が異なる値を返すとき、パーサーは再評価される
        var callCount = 0
        val parser = Parser<Int> { _, start ->
            callCount++
            ParseResult(callCount, start, start)
        }

        val context = StatefulParseContext("test")

        context.currentState = "A"
        val result1 = context.parseOrNull(parser, 0)
        assertEquals(1, result1?.value)
        assertEquals(1, callCount)

        // 状態が変わると同じ位置でも再評価される
        context.currentState = "B"
        val result2 = context.parseOrNull(parser, 0)
        assertEquals(2, result2?.value)
        assertEquals(2, callCount)
    }

    @Test
    fun sameStateReusesCachedResult() {
        // 以前と同じ状態に戻ったとき、キャッシュが再利用される
        var callCount = 0
        val parser = Parser<Int> { _, start ->
            callCount++
            ParseResult(callCount, start, start)
        }

        val context = StatefulParseContext("test")

        context.currentState = "A"
        context.parseOrNull(parser, 0)
        assertEquals(1, callCount)

        context.currentState = "B"
        context.parseOrNull(parser, 0)
        assertEquals(2, callCount)

        // 状態Aに戻る
        context.currentState = "A"
        val result = context.parseOrNull(parser, 0)
        assertEquals(1, result?.value) // 状態Aのキャッシュが返される
        assertEquals(2, callCount) // パーサーは再評価されない
    }

    @Test
    fun stateDependentParserProducesDifferentResults() {
        // 状態に応じて異なる結果を返すパーサーが、メモ化で正しく扱われる
        var mode = "upper"
        var callCount = 0

        val context = object : DefaultParseContext("abc") {
            override fun getState(): Any = mode
        }

        val parser = Parser<String> { ctx, start ->
            callCount++
            val ch = ctx.src[start].toString()
            ParseResult(if (mode == "upper") ch.uppercase() else ch.lowercase(), start, start + 1)
        }

        mode = "upper"
        val result1 = context.parseOrNull(parser, 0)
        assertEquals("A", result1?.value)
        assertEquals(1, callCount)

        mode = "lower"
        val result2 = context.parseOrNull(parser, 0)
        assertEquals("a", result2?.value)
        assertEquals(2, callCount) // 状態が変わったので再評価された

        mode = "upper"
        val result3 = context.parseOrNull(parser, 0)
        assertEquals("A", result3?.value)
        assertEquals(2, callCount) // キャッシュから返された
    }
}
