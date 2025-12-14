package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.or
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for errorPosition and suggestedParsers tracking in ParseContext.
 * 
 * These features help identify where parsing failed and which parsers
 * were attempted at the furthest position in the input.
 */
class ErrorTrackingTest {

    @Test
    fun errorPositionTracksSimpleFailure() {
        // errorPositionは、パースが失敗した最も遠い位置を追跡する
        val parser = +'a'
        val context = ParseContext("b", useCache = true)
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result)
        assertEquals(0, context.errorPosition) // 位置0で失敗
    }

    @Test
    fun errorPositionAdvancesWithLongerMatch() {
        // より長くマッチした位置でエラーが発生した場合、errorPositionが更新される
        // シーケンスパーサーを使用して、実際に位置を進める
        val parser = +"ab" * +'c'
        val context = ParseContext("abd", useCache = true)
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result)
        assertEquals(2, context.errorPosition) // "ab"まで進んでから位置2で'c'のパースに失敗
    }

    @Test
    fun errorPositionWithSequenceParsers() {
        // シーケンスパーサーでの失敗位置を追跡
        val parser = +'a' * +'b' * +'c'
        val context = ParseContext("abx", useCache = true)
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result)
        assertEquals(2, context.errorPosition) // "ab"の後、位置2で'c'のパースに失敗
    }

    @Test
    fun suggestedParsersCollectsSingleParser() {
        // 失敗したパーサーがsuggestedParsersに記録される
        val parser = +'a'
        val context = ParseContext("b", useCache = true)
        context.parseOrNull(parser, 0)
        
        assertEquals(1, context.suggestedParsers.size)
        assertTrue(context.suggestedParsers.contains(parser))
    }

    @Test
    fun suggestedParsersCollectsMultipleParsersAtSamePosition() {
        // 同じ位置で複数のパーサーが失敗した場合、すべて記録される
        val parser1 = +'a'
        val parser2 = +'b'
        val parser3 = +'c'
        val combined = or(parser1, parser2, parser3)
        
        val context = ParseContext("x", useCache = true)
        context.parseOrNull(combined, 0)
        
        // orコンビネーターを含めて4つのパーサーが記録される
        assertEquals(4, context.suggestedParsers.size)
        assertTrue(context.suggestedParsers.contains(parser1))
        assertTrue(context.suggestedParsers.contains(parser2))
        assertTrue(context.suggestedParsers.contains(parser3))
        assertTrue(context.suggestedParsers.contains(combined))
    }

    @Test
    fun suggestedParsersClearsWhenErrorPositionAdvances() {
        // errorPositionが進むと、古いsuggestedParsersがクリアされる
        val parser1 = +'x'  // 位置0で失敗するパーサー
        val parser2 = +'a' * +'b'  // 位置1まで進んでから失敗するパーサー
        
        val context = ParseContext("ac", useCache = true)
        
        // 最初に位置0で失敗
        context.parseOrNull(parser1, 0)
        assertEquals(0, context.errorPosition)
        assertEquals(1, context.suggestedParsers.size)
        
        // parser2が位置1まで進んでから失敗すると、suggestedParsersがクリアされて更新される
        context.parseOrNull(parser2, 0)
        assertEquals(1, context.errorPosition) // "a"の後の位置1で失敗
        
        // 位置1で失敗したパーサーのみが残る(以前のparser1はクリアされる)
        assertTrue(context.suggestedParsers.isNotEmpty())
        // parser1はもう含まれていない
        assertTrue(!context.suggestedParsers.contains(parser1))
    }

    @Test
    fun errorPositionWithNestedParsers() {
        // ネストされたパーサーでの失敗位置を追跡
        val digit = +Regex("[0-9]") map { it.value[0] }
        val twoDigits = digit * digit
        val parser = +'(' * twoDigits * +')'
        
        val context = ParseContext("(1x", useCache = true)
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result)
        assertEquals(2, context.errorPosition) // "(1"の後、位置2で2つ目の数字のパースに失敗
    }

    @Test
    fun errorTrackingWithCacheDisabled() {
        // キャッシュ無効時もerrorPositionとsuggestedParsersが正しく動作する
        val parser = +"ab" * +'c'
        val context = ParseContext("abd", useCache = false)
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result)
        assertEquals(2, context.errorPosition)
        assertTrue(context.suggestedParsers.isNotEmpty())
    }

    @Test
    fun errorPositionDoesNotRegressOnEarlierFailures() {
        // 早い位置での失敗はerrorPositionを後退させない
        val parser = +"ab" * +'c'
        val context = ParseContext("abx", useCache = true)
        
        // 最初のパースで位置2まで進む
        context.parseOrNull(parser, 0)
        assertEquals(2, context.errorPosition)
        val previousSuggestedCount = context.suggestedParsers.size
        
        // 位置0での失敗は、errorPositionを変更しない
        val earlyParser = +'x'
        context.parseOrNull(earlyParser, 0)
        assertEquals(2, context.errorPosition) // 変更なし
        assertEquals(previousSuggestedCount, context.suggestedParsers.size) // suggestedParsersも変更なし
    }

    @Test
    fun suggestedParsersAtEqualPosition() {
        // 同じerrorPositionでの失敗は、suggestedParsersに追加される
        val parser1 = +'a' * +'b'
        val context = ParseContext("ax", useCache = true)
        
        context.parseOrNull(parser1, 0)
        assertEquals(1, context.errorPosition)
        val firstCount = context.suggestedParsers.size
        
        // 同じ位置1での別の失敗
        val parser2 = +'a' * +'y'
        context.parseOrNull(parser2, 0)
        assertEquals(1, context.errorPosition)
        assertTrue(context.suggestedParsers.size >= firstCount) // 追加される
    }

    @Test
    fun errorPositionWithSuccessfulParse() {
        // 成功したパースはerrorPositionとsuggestedParsersに影響しない
        val parser = +"abc"
        val context = ParseContext("abc", useCache = true)
        val result = context.parseOrNull(parser, 0)
        
        assertEquals("abc", result?.value)
        assertEquals(0, context.errorPosition) // 初期値のまま
        assertEquals(0, context.suggestedParsers.size) // 空のまま
    }
}
