package io.github.mirrgieriana.xarpite.xarpeg

internal data class MatrixPosition(val row: Int, val column: Int)

internal fun createIndexToPositionConverter(src: String): (Int) -> MatrixPosition {
    val lineStartIndices = mutableListOf(0)
    src.forEachIndexed { index, char ->
        if (char == '\n') lineStartIndices.add(index + 1)
    }

    return { index ->
        require(index in 0..src.length) { "index ($index) is out of range for src of length ${src.length}" }

        val lineIndex = lineStartIndices.binarySearch(index).let { if (it >= 0) it else -it - 2 }
        val lineStart = lineStartIndices[lineIndex]
        MatrixPosition(row = lineIndex + 1, column = index - lineStart + 1)
    }
}
