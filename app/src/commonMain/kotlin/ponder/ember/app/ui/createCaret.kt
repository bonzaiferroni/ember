package ponder.ember.app.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlin.math.abs

internal fun WriterState.moveCaretLine(
    delta: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): Caret {
    require(abs(delta) == 1) { "line caret delta must be 1 or -1" }
    val blocks = blocks
    val block = blocks[caret.blockIndex]
    var newLineIndex = caret.lineIndex + delta
    val textIndex = if (newLineIndex < 0) {
        if (caret.blockIndex == 0) return Caret.Home
        val block = blocks[caret.blockIndex - 1]
        val line = block.lines.last()
        newLineIndex = line.lineIndex
        findNearestTextIndex(block.blockIndex, line.lineIndex, caret.preferredOffsetX, ruler, style)
    } else if (newLineIndex >= block.lines.size) {
        if (caret.blockIndex + 1 >= blocks.size) {
            newLineIndex = caret.lineIndex
            text.length
        } else {
            newLineIndex = 0
            findNearestTextIndex(caret.blockIndex + 1, 0, caret.preferredOffsetX, ruler, style)
        }
    } else {
        findNearestTextIndex(caret.blockIndex, newLineIndex, caret.preferredOffsetX, ruler, style)
    }
    return createCaretAtIndex(textIndex, newLineIndex, ruler, style, spacePx, false)
}

internal fun WriterState.createCaretAtIndex(
    textIndex: Int,
    lineIndex: Int?,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int,
    setPreferred: Boolean
): Caret {
    val block = blocks.first { it.endTextIndex >= textIndex }
    val blockTextIndex = textIndex - block.textIndex
    val line = lineIndex?.let { block.lines[it] }
        ?: block.lines.firstOrNull { it.endBlockTextIndex >= blockTextIndex }
    val lineTextIndex = line?.blockTextIndex ?: blockTextIndex
    val chunk = block.chunks.firstOrNull { it.endBlockTextIndex >= blockTextIndex }
    var chunkIndex = chunk?.chunkIndex ?: 0
    var offsetX = chunk?.let {
        ruler.measure(chunk.text.take(blockTextIndex - chunk.blockTextIndex), style).size.width
    } ?: 0
    var offsetIndex = chunk?.blockTextIndex ?: blockTextIndex
    while (offsetIndex > lineTextIndex) {
        val lineChunk = block.chunks[--chunkIndex]
        offsetX += lineChunk.textLayout.size.width + spacePx
        offsetIndex = lineChunk.blockTextIndex
    }
    return Caret(
        textIndex = textIndex,
        blockTextIndex = blockTextIndex,
        blockIndex = block.blockIndex,
        lineIndex = line?.lineIndex ?: 0,
        chunkIndex = chunk?.chunkIndex ?: 0,
        offsetX = offsetX,
        preferredOffsetX = if (setPreferred) offsetX else caret.preferredOffsetX
    )
}

internal fun WriterState.setCaretAtIndex(
    textIndex: Int,
    lineIndex: Int?,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): Caret {
    val index = (textIndex).coerceIn(0, text.length)
    return createCaretAtIndex(index, lineIndex, ruler, style, spacePx, true)
}

internal fun WriterState.findNearestTextIndex(
    blockIndex: Int,
    lineIndex: Int,
    targetX: Int,
    ruler: TextMeasurer,
    style: TextStyle,
): Int {
    val block = blocks[blockIndex]
    val chunk = block.chunks.firstOrNull {
        it.lineIndex == lineIndex && it.endOffsetX >= targetX
    } ?: block.chunks.lastOrNull() { it.lineIndex == lineIndex } ?: return block.textIndex
    if (targetX >= chunk.endOffsetX) return block.textIndex + chunk.endBlockTextIndex
    var blockTextIndex = chunk.blockTextIndex
    var lastSubWidth = 0
    while (blockTextIndex < chunk.endBlockTextIndex) {
        val nextCharIndex = blockTextIndex + 1 - chunk.blockTextIndex
        val subWidth = ruler.measure(chunk.text.take(nextCharIndex), style).size.width
        if (subWidth + chunk.offsetX > targetX) {
            if ((subWidth + chunk.offsetX) - targetX < targetX - (lastSubWidth + chunk.offsetX)) {
                blockTextIndex++
            }
            break
        }
        lastSubWidth = subWidth
        blockTextIndex++
    }
    return block.textIndex + blockTextIndex
}

fun String.insertAt(i: Int, text: String): String =
    StringBuilder(this).insert(i, text).toString()

fun String.removeRange(startIndex: Int, endIndex: Int): String {
    require(startIndex in 0..length && endIndex in startIndex..length) {
        "Invalid range: $startIndex..$endIndex for length $length"
    }
    return substring(0, startIndex) + substring(endIndex)
}