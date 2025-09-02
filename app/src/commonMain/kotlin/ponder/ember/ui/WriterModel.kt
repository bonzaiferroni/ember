package ponder.ember.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlin.math.abs

internal class WriterModel(
    val ruler: TextMeasurer,
    val style: TextStyle,
    val blockWidthPx: Int,
    val spacePx: Int,
) {
    var content: WriterContent = WriterContent("", emptyList())

    fun create(text: String): WriterContent {
        var startIndex = 0
        val blocks = text.split('\n').map { blockText ->
            val blockContent = content.blocks.firstOrNull { it.text == blockText }?.copy(textIndex = startIndex)
                ?: parseBlockContent(blockText, startIndex, ruler, style, blockWidthPx, spacePx)
            startIndex += blockText.length + 1
            blockContent
        }
        return WriterContent(
            text = text,
            blocks = blocks,
        ).also { content = it }
    }
}

internal data class WriterContent(
    val text: String,
    val blocks: List<BlockContent>,
)

@Stable
internal data class BlockContent(
    val text: String,
    val chunks: List<WriterChunk>,
    val lines: List<WriterLine>,
    val textIndex: Int,
) {
    val endTextIndex get() = textIndex + text.length
}

internal data class WriterChunk(
    val text: String,
    val textLayout: TextLayoutResult,
    val textIndex: Int,
    val lineIndex: Int,
    val offsetX: Int,
    val isContinued: Boolean,
) {
    val endTextIndex get() = textIndex + text.length
}

internal data class WriterLine(
    val textIndex: Int,
    val lineIndex: Int,
    val length: Int,
    val chunkIndex: Int,
    val chunkCount: Int,
) {
    val endChunkIndex get() = chunkIndex + chunkCount
    val endTextIndex get() = textIndex + length
}

internal data class WriterCursor(
    val index: Int,
    val offsetX: Int,
    val preferredOffsetX: Int
)

internal fun WriterModel.moveCursorLine(cursor: WriterCursor, delta: Int): WriterCursor {
    require(abs(delta) == 1) { "line cursor delta must be 1 or -1" }
    val blocks = content.blocks
    val currentBlock = blocks.first { cursor.index >= it.textIndex }
    val currentLine = currentBlock.lines.first { cursor.index >= it.textIndex }
    val blockIndex = blocks.indexOfFirst { currentBlock.textIndex == it.textIndex }
    val lineIndex = currentBlock.lines.indexOfFirst { currentLine.textIndex == it.textIndex } + delta
    if (lineIndex < 0) {
        if (blockIndex == 0) return WriterCursor(
            index = 0,
            offsetX = 0,
            preferredOffsetX = 0
        )
        val block = blocks[blockIndex - 1]
        val line = block.lines[block.lines.size - 1]
        return putCursorOnLine(cursor, line.chunkIndex)
    }
    return cursor // incomplete
}

internal fun WriterModel.indexOffsetX(index: Int): Int {
    val block = content.blocks.first { index >= it.textIndex }
    val line = block.lines.first { index >= it.textIndex }
    val chunk = block.chunks.first { index >= it.textIndex }
    var chunkIndex = block.chunks.indexOfFirst { it.textIndex == chunk.textIndex }
    var offsetX = ruler.measure(chunk.text.take(index - chunk.textIndex), style).size.width
    var offsetIndex = chunk.textIndex
    while (offsetIndex > line.textIndex) {
        val lineChunk = block.chunks[--chunkIndex]
        offsetX += lineChunk.textLayout.size.width + spacePx
        offsetIndex = lineChunk.textIndex
    }
    return offsetX
}

internal fun WriterModel.moveCursorIndex(cursor: WriterCursor, delta: Int): WriterCursor {
    val index = (cursor.index + delta).coerceIn(0, content.text.length)
    val offsetX = indexOffsetX(index)
    return WriterCursor(
        index = index,
        offsetX = offsetX,
        preferredOffsetX = offsetX
    )
}

internal fun WriterModel.putCursorOnLine(cursor: WriterCursor, textIndex: Int): WriterCursor {
    val block = content.blocks.first { textIndex >= it.textIndex }
    val line = block.lines.first { textIndex >= it.textIndex }
    val chunk = block.chunks.firstOrNull {
        textIndex >= it.textIndex
                && it.offsetX + it.textLayout.size.width >= cursor.preferredOffsetX
                && it.lineIndex == line.lineIndex
    } ?: block.chunks.last { it.lineIndex == line.lineIndex }
    return cursor // incomplete
}