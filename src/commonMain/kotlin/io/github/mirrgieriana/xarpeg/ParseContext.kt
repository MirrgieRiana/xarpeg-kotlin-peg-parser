package io.github.mirrgieriana.xarpeg

/**
 * パース操作のコンテキスト。
 *
 * メモ化キャッシュ、エラー追跡、位置情報を含むパース状態を管理します。
 *
 * @param src パース対象の入力文字列。
 * @param useMemoization バックトラッキング時のパフォーマンス向上のためにパース結果をキャッシュするかどうか。
 */
class ParseContext(val src: String, val useMemoization: Boolean) {

    private val memo = mutableMapOf<Pair<Parser<*>, Int>, ParseResult<Any>?>()

    /**
     * 現在名前付きパーサーの内部にいるかどうか。
     *
     * このフラグは、ネストされた名前付きパーサーがエラー追跡を妨げるのを防ぎます。
     */
    var isInNamedParser = false
    
    /**
     * パースが失敗した最も遠い位置。
     *
     * 入力内で到達した最深点を追跡することで、意味のあるエラーメッセージを提供するために使用されます。
     */
    var errorPosition: Int = 0
    
    /**
     * [errorPosition]でマッチできた可能性のあるパーサーのセット。
     *
     * 失敗地点で期待されていた内容を示す有用なエラーメッセージを生成するために使用されます。
     */
    val suggestedParsers = mutableSetOf<Parser<*>>()

    private val matrixPositionCalculator by lazy { MatrixPositionCalculator(src) }
    
    /**
     * 線形位置を(行、列)位置に変換します。
     *
     * @param index 入力文字列内の位置。
     * @return 行番号と列番号を含む[MatrixPosition]（1から始まるインデックス）。
     */
    fun toMatrixPosition(index: Int) = matrixPositionCalculator.toMatrixPosition(index)
    
    /**
     * パースエラーの(行、列)位置。
     */
    val errorMatrixPosition get() = toMatrixPosition(errorPosition)

    /**
     * 指定された位置で与えられたパーサーを使用してパースを試みます。
     *
     * このメソッドはメモ化とエラー追跡を自動的に処理します。
     *
     * @param parser 使用するパーサー。
     * @param start 入力内の開始位置。
     * @return パース成功時は[ParseResult]、失敗時は`null`。
     */
    fun <T : Any> parseOrNull(parser: Parser<T>, start: Int): ParseResult<T>? {
        val result = if (useMemoization) {
            val key = Pair(parser, start)
            if (key in memo) {
                @Suppress("UNCHECKED_CAST")
                memo[key] as ParseResult<T>?
            } else {
                val result = if (!isInNamedParser && parser.name != null) {
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
                memo[key] = result
                result
            }
        } else {
            if (!isInNamedParser && parser.name != null) {
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
        if (result == null && !isInNamedParser && start >= errorPosition) {
            if (start > errorPosition) {
                errorPosition = start
                suggestedParsers.clear()
            }
            // Only add parsers with names to suggestions - unnamed parsers are just noise
            if (parser.name != null) {
                suggestedParsers += parser
            }
        }
        return result
    }

}

/**
 * 入力内の位置を(行、列)として表します。
 *
 * 行と列は両方とも1から始まるインデックスです。
 *
 * @param row 行番号（1から始まるインデックス）。
 * @param column 行内の列番号（1から始まるインデックス）。
 */
data class MatrixPosition(val row: Int, val column: Int)

/**
 * 線形文字列位置を(行、列)位置に変換します。
 *
 * 改行を効率的に追跡し、文字列インデックスから人間が読みやすい位置への迅速な変換を可能にします。
 *
 * @param src 解析する入力文字列。
 */
class MatrixPositionCalculator(private val src: String) {
    private val lineStartIndices = run {
        val list = mutableListOf(0)
        src.forEachIndexed { index, char ->
            if (char == '\n') list.add(index + 1)
        }
        list
    }

    /**
     * 線形位置を(行、列)形式に変換します。
     *
     * @param index ソース文字列内の位置（0から始まるインデックス）。
     * @return 1から始まるインデックスの行番号と列番号を持つ[MatrixPosition]。
     * @throws IllegalArgumentException インデックスが範囲外の場合。
     */
    fun toMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }
}
