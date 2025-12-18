package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import kotlin.test.Test

class ParserFailureBehaviorTest {
    @Test
    fun verifyTupleParserFailureBehavior() {
        println("=== タプルパーサー（combine）の失敗時の挙動 ===\n")
        
        // タプルパーサーを使った構造: 'a' * 'b' * 'c'
        val parser = 'a'.toParser() * 'b'.toParser() * 'c'.toParser() * endOfInput
        
        println("1. パーサーの構造:")
        println("   'a' * 'b' * 'c' * endOfInput")
        
        // ケース1: 最初の'a'で失敗
        println("\n2. ケース1: 入力 'x' (最初の'a'で失敗)")
        val result1 = parser.parseAll("x")
        val ex1 = result1.exceptionOrNull() as? ParseException
        println("   エラー位置: ${ex1?.context?.errorPosition}")
        println("   サジェスト:")
        ex1?.context?.suggestedParsers?.forEachIndexed { i, p ->
            val className = p.javaClass.name
            val isCombine = className.contains("combine")
            val label = if (isCombine) " ← combine関数が生成したタプルパーサー" else ""
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'$label")
        }
        
        // ケース2: 'a'は成功、'b'で失敗
        println("\n3. ケース2: 入力 'ax' ('b'で失敗)")
        val result2 = parser.parseAll("ax")
        val ex2 = result2.exceptionOrNull() as? ParseException
        println("   エラー位置: ${ex2?.context?.errorPosition}")
        println("   サジェスト:")
        ex2?.context?.suggestedParsers?.forEachIndexed { i, p ->
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'")
        }
        
        // ケース3: 'a','b'は成功、'c'で失敗
        println("\n4. ケース3: 入力 'abx' ('c'で失敗)")
        val result3 = parser.parseAll("abx")
        val ex3 = result3.exceptionOrNull() as? ParseException
        println("   エラー位置: ${ex3?.context?.errorPosition}")
        println("   サジェスト:")
        ex3?.context?.suggestedParsers?.forEachIndexed { i, p ->
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'")
        }
        
        println("\n5. 結論:")
        println("   タプルパーサー（combine）自体もサジェストに追加される。")
        println("   しかし、それらは name=null で、formatParseExceptionでフィルタされる。")
        println("   実際にエラーメッセージに表示されるのは名前付きパーサーのみ。")
    }
    
    @Test
    fun verifyOrParserFailureBehavior() {
        println("\n=== Orパーサーの失敗時の挙動 ===\n")
        
        // Orパーサーを使った構造: 'a' + 'b' + 'c'
        val parser = ('a'.toParser() + 'b'.toParser() + 'c'.toParser()) * endOfInput
        
        println("1. パーサーの構造:")
        println("   ('a' + 'b' + 'c') * endOfInput")
        
        // どれにもマッチしない入力
        println("\n2. 入力 'x' (全ての選択肢で失敗)")
        val result = parser.parseAll("x")
        val ex = result.exceptionOrNull() as? ParseException
        println("   エラー位置: ${ex?.context?.errorPosition}")
        println("   サジェスト:")
        ex?.context?.suggestedParsers?.forEachIndexed { i, p ->
            val className = p.javaClass.name
            val label = when {
                className.contains("CharParser") -> " ← Orの選択肢"
                className.contains("OrParser") -> " ← Orパーサー自身"
                className.contains("combine") -> " ← combine関数が生成したタプルパーサー（Or * endOfInput）"
                else -> ""
            }
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'$label")
        }
        
        println("\n3. Orパーサー自体の名前:")
        val orParser = 'a'.toParser() + 'b'.toParser() + 'c'.toParser()
        println("   orParser.name = ${orParser.name}")
        
        println("\n4. 結論:")
        println("   Orパーサーは全ての選択肢を試すため、")
        println("   全ての選択肢の葉パーサー（CharParser）がサジェストに現れる。")
        println("   Orパーサー自体も name=null でサジェストに入る。")
        println("   さらに、Orと endOfInput を結合する combine パーサーも name=null でサジェストに入る。")
        println("   しかし、name=null のパーサーは formatParseException でフィルタされる。")
    }
    
    @Test
    fun verifyMappedParserWithinCombine() {
        println("\n=== combine内でmapされたパーサーの挙動 ===\n")
        
        // -'(' * number * -')' のような構造
        val openParen = -'('
        val number = +Regex("[0-9]+")
        val closeParen = -')'
        val parser = openParen * number * closeParen * endOfInput
        
        println("1. パーサーの構造:")
        println("   -'(' * +Regex(\"[0-9]+\") * -')' * endOfInput")
        println("   各パーサーの名前:")
        println("     openParen.name = ${openParen.name}")
        println("     number.name = ${number.name}")
        println("     closeParen.name = ${closeParen.name}")
        
        // '('で失敗
        println("\n2. ケース1: 入力 'x' ('('で失敗)")
        val result1 = parser.parseAll("x")
        val ex1 = result1.exceptionOrNull() as? ParseException
        println("   サジェスト:")
        ex1?.context?.suggestedParsers?.forEachIndexed { i, p ->
            val className = p.javaClass.name
            val isCombine = className.contains("combine")
            val label = if (isCombine) " ← combineパーサー" else ""
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'$label")
        }
        
        // ')'で失敗
        println("\n3. ケース2: 入力 '(123' (')'で失敗)")
        val result2 = parser.parseAll("(123")
        val ex2 = result2.exceptionOrNull() as? ParseException
        println("   サジェスト:")
        ex2?.context?.suggestedParsers?.forEachIndexed { i, p ->
            println("     [$i] ${p.javaClass.simpleName.padEnd(25)} name='${p.name}'")
        }
        
        println("\n4. 重要な発見:")
        println("   mapで名前を保持しているため、-'('や-')'は名前付きパーサーとして認識される。")
        println("   これにより、その内部のCharParserはサジェストに追加されず、")
        println("   重複が防がれる。")
        println("   combineパーサー（name=null）もサジェストに入るが、")
        println("   formatParseExceptionでフィルタされる。")
    }
}
