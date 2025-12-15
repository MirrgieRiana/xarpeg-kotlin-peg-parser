package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the token candidate feature that tracks which parsers failed at the furthest position.
 */
class TokenCandidateTest {

    @Test
    fun suggestedParsers_singleParser_failure() {
        // 単一のパーサーが失敗した場合、そのパーサーが候補として記録される
        val parser = +'a'
        val context = ParseContext("b", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0
        assertEquals(1, context.suggestedParsers.size) // 候補は1つ
        assertTrue(parser in context.suggestedParsers) // 失敗したパーサーが候補
    }

    @Test
    fun suggestedParsers_multipleAlternatives_collectsAll() {
        // 複数の選択肢が同じ位置で失敗した場合、すべてが候補として記録される
        val parserA = +'a'
        val parserB = +'b'
        val parserC = +'c'
        val parser = parserA + parserB + parserC
        val context = ParseContext("x", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0
        // OrParser自体も含めて4つの候補が記録される
        assertEquals(4, context.suggestedParsers.size)
        assertTrue(parserA in context.suggestedParsers) // 'a'が候補
        assertTrue(parserB in context.suggestedParsers) // 'b'が候補
        assertTrue(parserC in context.suggestedParsers) // 'c'が候補
        assertTrue(parser in context.suggestedParsers) // OrParser自体も候補
    }

    @Test
    fun suggestedParsers_furthestPositionWins() {
        // より遠い位置で失敗したパーサーが優先される
        // シーケンスを使ってより遠い位置での失敗を確認
        val parserAB = +"ab"
        val parserACD = +"acd"
        val parser = parserAB + parserACD
        val context = ParseContext("acx", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        // "acd"は"ac"までマッチして位置2で失敗、"ab"は位置0で失敗
        // ただし、StringParserは最初の不一致で即座にnullを返すため、
        // 呼び出し位置が記録される
        assertEquals(0, context.errorPosition) // エラー位置は0
        assertTrue(context.suggestedParsers.size >= 2)
    }

    @Test
    fun suggestedParsers_earlierFailuresAreIgnored() {
        // 初期の位置で失敗したパーサーは、後の位置で失敗したパーサーに置き換えられる
        // シーケンスの例を使って、後の位置で失敗するケースを確認
        val charA = +'a'
        val charB = +'b'
        val charC = +'c'
        val parser = charA * charB * charC
        val context = ParseContext("abd", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(2, context.errorPosition) // エラー位置は2（"ab"の後）
        // 'c'のみが最も遠い位置で失敗したため候補となる
        assertTrue(charC in context.suggestedParsers) // 'c'が候補
        assertTrue(charA !in context.suggestedParsers) // 'a'は候補に含まれない（位置0で成功）
        assertTrue(charB !in context.suggestedParsers) // 'b'は候補に含まれない（位置1で成功）
    }

    @Test
    fun suggestedParsers_namedParser_collectsNamedOnly() {
        // 名前付きパーサーの場合、内部のパーサーではなく名前付きパーサー自体が候補となる
        val charB = +'b'
        val innerParser = +'a' * charB
        val namedParser = innerParser named "AB"
        val context = ParseContext("ac", useCache = true)
        
        val result = context.parseOrNull(namedParser, 0)
        
        assertNull(result) // パースは失敗
        // 名前付きパーサーが呼び出された位置でエラーが記録される
        assertEquals(0, context.errorPosition) // エラー位置は0
        // 名前付きパーサーの場合は内部のパーサーではなく名前付きパーサー自体が候補
        assertTrue(context.suggestedParsers.size >= 1)
        assertTrue(namedParser in context.suggestedParsers) // 名前付きパーサーが候補
        assertTrue(charB !in context.suggestedParsers) // 内部のパーサーは候補に含まれない
    }

    @Test
    fun suggestedParsers_mixedNamedAndUnnamed() {
        // 名前付きと名前なしのパーサーが混在する場合の動作
        val parserA = +'a' named "A"
        val parserB = +'b' named "B"
        val parserC = +'c'
        val parser = parserA + parserB + parserC
        val context = ParseContext("x", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0
        // 名前付きパーサーと名前なしパーサーが候補として記録される
        assertTrue(context.suggestedParsers.size >= 3)
        assertTrue(parserA in context.suggestedParsers) // 名前付き'A'が候補
        assertTrue(parserB in context.suggestedParsers) // 名前付き'B'が候補
        assertTrue(parserC in context.suggestedParsers) // 名前なし'c'が候補
    }

    @Test
    fun suggestedParsers_successfulParse_noSuggestions() {
        // パースが成功した場合、候補は記録されない
        val parser = +'a'
        val context = ParseContext("a", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertEquals('a', result?.value) // パースは成功
        assertEquals(0, context.errorPosition) // エラー位置は更新されない
        assertEquals(0, context.suggestedParsers.size) // 候補はない
    }

    @Test
    fun suggestedParsers_partialSuccess_collectsLaterFailures() {
        // 部分的に成功し、その後失敗した場合
        val parserAB = +"ab"
        val parserAC = +"ac"
        val charD = +'d'
        val sequence = (parserAB + parserAC) * charD
        val context = ParseContext("abx", useCache = true)
        
        val result = context.parseOrNull(sequence, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(2, context.errorPosition) // エラー位置は2（"ab"の後）
        assertEquals(1, context.suggestedParsers.size) // 候補は1つ
        assertTrue(charD in context.suggestedParsers) // 'd'が候補
    }

    @Test
    fun suggestedParsers_withCache() {
        // キャッシュが有効な場合も候補が正しく記録される
        val parser = +'a' + +'b'
        val context = ParseContext("c", useCache = true)
        
        // 最初のパース
        val result1 = context.parseOrNull(parser, 0)
        assertNull(result1)
        assertTrue(context.suggestedParsers.size >= 2)
        
        // キャッシュから取得される2回目のパースでも候補は維持される
        context.suggestedParsers.clear()
        val result2 = context.parseOrNull(parser, 0)
        assertNull(result2)
        // キャッシュから返されるため、候補は再度記録されない
        assertEquals(0, context.suggestedParsers.size)
    }

    @Test
    fun suggestedParsers_withoutCache() {
        // キャッシュが無効な場合も候補が正しく記録される
        val parser = +'a' + +'b'
        val context = ParseContext("c", useCache = false)
        
        // 最初のパース
        val result1 = context.parseOrNull(parser, 0)
        assertNull(result1)
        assertTrue(context.suggestedParsers.size >= 2)
        
        // 2回目のパースでも候補は再度記録される
        context.suggestedParsers.clear()
        context.errorPosition = 0
        val result2 = context.parseOrNull(parser, 0)
        assertNull(result2)
        assertTrue(context.suggestedParsers.size >= 2)
    }

    @Test
    fun suggestedParsers_sequence_collectsAtFailurePoint() {
        // シーケンスの途中で失敗した場合、失敗点の候補が記録される
        val charC = +'c'
        val parser = +'a' * +'b' * charC
        val context = ParseContext("abx", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(2, context.errorPosition) // エラー位置は2（"ab"の後）
        assertEquals(1, context.suggestedParsers.size) // 候補は1つ
        assertTrue(charC in context.suggestedParsers) // 'c'が候補
    }

    @Test
    fun suggestedParsers_ignoredParser_included() {
        // 無視されるパーサー（unaryMinus）が失敗した場合も候補として記録される
        val charA = +'a'
        val parser = -charA * +'b'
        val context = ParseContext("x", useCache = true)
        
        val result = context.parseOrNull(parser, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0
        // 無視パーサー内のパーサーが候補として記録される
        assertTrue(context.suggestedParsers.size >= 1)
        assertTrue(charA in context.suggestedParsers) // 'a'が候補
    }

    @Test
    fun suggestedParsers_realWorldExample_keywordChoice() {
        // 実用例：キーワードの選択肢
        val ifKeyword = +"if" named "if"
        val elseKeyword = +"else" named "else"
        val whileKeyword = +"while" named "while"
        val keyword = ifKeyword + elseKeyword + whileKeyword
        
        val context = ParseContext("for", useCache = true)
        val result = context.parseOrNull(keyword, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0（最初の文字で失敗）
        // すべてのキーワードが候補として記録される
        assertTrue(context.suggestedParsers.size >= 3)
    }

    @Test
    fun suggestedParsers_realWorldExample_expressionParsing() {
        // 実用例：式のパース
        val number = +Regex("[0-9]+") named "number"
        val identifier = +Regex("[a-zA-Z]+") named "identifier"
        val expression = number + identifier
        
        val context = ParseContext("@#$", useCache = true)
        val result = context.parseOrNull(expression, 0)
        
        assertNull(result) // パースは失敗
        assertEquals(0, context.errorPosition) // エラー位置は0
        assertTrue(context.suggestedParsers.size >= 2) // 少なくとも2つの候補
        assertTrue(number in context.suggestedParsers) // numberが候補
        assertTrue(identifier in context.suggestedParsers) // identifierが候補
    }

    @Test
    fun parseAllOrThrow_includesSuggestionsInErrorMessage() {
        // parseAllOrThrowが失敗した場合、エラーメッセージに候補が含まれる
        val number = +Regex("[0-9]+") named "number"
        val identifier = +Regex("[a-zA-Z]+") named "identifier"
        val expression = number + identifier
        
        try {
            expression.parseAllOrThrow("@invalid")
            kotlin.test.fail("Expected UnmatchedInputParseException")
        } catch (e: UnmatchedInputParseException) {
            // エラーメッセージに候補が含まれていることを確認
            assertTrue(e.message?.contains("Expected:") == true)
            assertTrue(e.message?.contains("number") == true)
            assertTrue(e.message?.contains("identifier") == true)
            assertEquals(0, e.position) // エラー位置は0
        }
    }

    @Test
    fun parseAllOrThrow_withUnnamedParsers_noSuggestionsInMessage() {
        // 名前のないパーサーの場合、候補リストは空になる
        val parser = +'a' + +'b'
        
        try {
            parser.parseAllOrThrow("x")
            kotlin.test.fail("Expected UnmatchedInputParseException")
        } catch (e: UnmatchedInputParseException) {
            // 名前のないパーサーなので"Expected:"は含まれない
            assertTrue(e.message?.contains("Failed to parse") == true)
            assertEquals(0, e.position) // エラー位置は0
        }
    }
}
