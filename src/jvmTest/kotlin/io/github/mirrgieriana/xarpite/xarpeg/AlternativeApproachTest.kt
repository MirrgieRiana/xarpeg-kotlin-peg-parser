package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import kotlin.test.Test

class AlternativeApproachTest {
    @Test
    fun compareApproaches() {
        println("=== 2つのアプローチの比較 ===\n")
        
        println("【現在の修正】map関数でnameを保持")
        println("  メリット:")
        println("    - mapされたパーサーが名前を持つため、エラーメッセージに表示される")
        println("    - isInNamedParserフラグにより、内部のCharParserはサジェストから除外される")
        println("  デメリット:")
        println("    - map/mapEx関数の実装を変更する必要がある")
        println()
        
        println("【代替案】ParseContextでname!=nullのみ追加")
        println("  メリット:")
        println("    - より根本的な解決：全ての名無しパーサーがサジェストから除外される")
        println("    - map関数を変更する必要がない")
        println("    - formatParseExceptionでmapNotNullする必要もなくなる")
        println("  デメリット:")
        println("    - ???")
        println()
        
        println("【検証】代替案の問題点を確認")
        
        val parser = -'(' * +Regex("[0-9]+") * -')' * endOfInput
        
        println("\n1. 現在の実装での動作:")
        println("   パーサー構造: -'(' * +Regex(\"[0-9]+\") * -')' * endOfInput")
        println("   openParen.name = ${(-'(').name}")
        println("   closeParen.name = ${(-')').name}")
        println()
        println("   '(123' を解析（')'で失敗）:")
        val result = parser.parseAll("(123")
        val ex = result.exceptionOrNull() as? ParseException
        println("   サジェスト数: ${ex?.context?.suggestedParsers?.size}")
        ex?.context?.suggestedParsers?.forEach { p ->
            println("     - ${p.javaClass.simpleName.padEnd(30)} name='${p.name}'")
        }
        println("   表示される名前: ${ex?.context?.suggestedParsers?.mapNotNull { it.name }?.distinct()}")
        println()
        
        println("2. 代替案での動作予測:")
        println("   if (parser.name != null) suggestedParsers += parser")
        println("   →名前なしパーサーは最初からサジェストに追加されない")
        println("   →mapで名前を保持していれば、-')'がサジェストに入る")
        println("   →mapで名前を保持していなければ、内部の')'CharParserがサジェストに入る")
        println()
        println("   つまり、どちらにしてもmap修正は必要！")
        println("   ただし、代替案ではformatParseExceptionでmapNotNullが不要になる")
        println()
        
        println("【結論】")
        println("  代替案の方が優れている：")
        println("  - 名無しパーサーがサジェストに入らないため、メモリ効率が良い")
        println("  - formatParseExceptionの処理が簡潔になる")
        println("  - 意味的にも明確：「名前付きパーサーのみをサジェスト対象とする」")
        println()
        println("  両方の修正を組み合わせるのがベスト：")
        println("  1. ParseContext: name!=nullのみサジェストに追加")
        println("  2. MapParser: 名前を保持する（既に実装済み）")
    }
}
