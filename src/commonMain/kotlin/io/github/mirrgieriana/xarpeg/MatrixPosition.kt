package io.github.mirrgieriana.xarpeg

data class MatrixPosition(val row: Int, val column: Int)

class MatrixPositionCalculator(val src: String) {
    private val lineStartIndices = mutableListOf<Int>()
    private val lineExclusiveEndIndices = mutableListOf<Int>()

    init {
        lineStartIndices += 0
        var result = """\n|\r\n?""".toRegex().find(src)
        while (result != null) {
            lineExclusiveEndIndices += result.range.first
            lineStartIndices += result.range.last + 1
            result = result.next()
        }
        lineExclusiveEndIndices += src.length
    }

    fun getLineRange(lineNumber: Int): IntRange {
        require(lineNumber in 1..lineStartIndices.size) { "lineNumber ($lineNumber) is out of range (1..${lineStartIndices.size})" }
        val lineIndex = lineNumber - 1
        return lineStartIndices[lineIndex] until lineExclusiveEndIndices[lineIndex]
    }

    fun getMatrixPosition(index: Int): MatrixPosition {
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }
        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else (-it - 1) - 1 }
        val lineStartIndex = lineStartIndices[lineIndex]
        return MatrixPosition(row = lineIndex + 1, column = index - lineStartIndex + 1)
    }

}
