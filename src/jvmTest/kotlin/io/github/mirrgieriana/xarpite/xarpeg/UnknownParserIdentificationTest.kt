package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import kotlin.test.Test

class UnknownParserIdentificationTest {
    @Test
    fun identifyUnknownParserInOrParserSuggestions() {
        println("=== Orパーサーのサジェストに現れる名無しパーサーの正体を特定 ===\n")
        
        val parser = ('a'.toParser() + 'b'.toParser() + 'c'.toParser()) * endOfInput
        
        println("1. パーサーの構造:")
        println("   ('a' + 'b' + 'c') * endOfInput")
        
        val result = parser.parseAll("x")
        val ex = result.exceptionOrNull() as? ParseException
        
        println("\n2. 入力 'x' のサジェスト（詳細版）:")
        ex?.context?.suggestedParsers?.forEachIndexed { i, p ->
            println("   [$i]")
            println("      クラス: ${p.javaClass.name}")
            println("      簡易名: ${p.javaClass.simpleName}")
            println("      name: '${p.name}'")
            println("      toString: ${p.toString()}")
            println("      identityHashCode: ${System.identityHashCode(p)}")
        }
        
        println("\n3. 各パーサーの正体確認:")
        val orParser = 'a'.toParser() + 'b'.toParser() + 'c'.toParser()
        val combined = orParser * endOfInput
        println("   orParser: ${orParser.javaClass.name}, name='${orParser.name}'")
        println("   combined: ${combined.javaClass.name}, name='${combined.name}'")
        println("   endOfInput: ${endOfInput.javaClass.name}, name='${endOfInput.name}'")
        
        println("\n4. サジェスト内のパーサーとの照合:")
        ex?.context?.suggestedParsers?.forEachIndexed { i, p ->
            val match = when {
                p === endOfInput -> "endOfInput"
                p === orParser -> "orParser"
                p === combined -> "combined"
                p.javaClass.simpleName == "CharParser" -> "CharParser"
                p.javaClass.simpleName == "OrParser" -> "OrParser"
                else -> "不明"
            }
            println("   [$i] ${p.javaClass.simpleName.padEnd(25)} → $match")
        }
        
        println("\n5. combineパーサー（times演算子）の正体:")
        println("   combine自体も name=null で、サジェストに追加される可能性がある")
    }
    
    @Test
    fun identifyAllParsersInTupleSuggestions() {
        println("\n=== タプルパーサーのサジェストに現れる名無しパーサーの正体 ===\n")
        
        val parser = 'a'.toParser() * 'b'.toParser() * 'c'.toParser() * endOfInput
        
        println("1. パーサーの構造:")
        println("   'a' * 'b' * 'c' * endOfInput")
        
        val result = parser.parseAll("x")
        val ex = result.exceptionOrNull() as? ParseException
        
        println("\n2. 入力 'x' のサジェスト（詳細版）:")
        ex?.context?.suggestedParsers?.forEachIndexed { i, p ->
            println("   [$i]")
            println("      クラス: ${p.javaClass.name}")
            println("      簡易名: ${p.javaClass.simpleName}")
            println("      name: '${p.name}'")
        }
        
        println("\n3. 名無しパーサーの正体:")
        println("   これらはcombine関数で生成された中間パーサー（タプルパーサー）")
        println("   'a' * 'b' → combine(a, b) → 名無しパーサー")
        println("   (a*b) * 'c' → combine(a*b, c) → 名無しパーサー")
        println("   ((a*b)*c) * endOfInput → combine((a*b)*c, endOfInput) → 名無しパーサー")
    }
}
