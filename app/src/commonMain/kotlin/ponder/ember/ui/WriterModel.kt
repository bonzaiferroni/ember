package ponder.ember.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

internal class WriterModel(
    val ruler: TextMeasurer,
    val style: TextStyle,
    val spacePx: Int,
) {
    private val state = MutableStateFlow(WriterState())
    internal val stateNow get() = state.value
    val stateFlow: StateFlow<WriterState> = state

    private fun setState(block: (WriterState) -> WriterState) {
        state.value = block(state.value)
    }

    private var cursorTarget = 0

    fun updateContent(text: String, blockWidthPx: Int) {
        if (text == stateNow.text && blockWidthPx == stateNow.blockWidthPx) return

        var textIndex = 0
        val blocks = text.split('\n').mapIndexed { blockIndex, blockText ->
            val blockContent = stateNow.blocks.takeIf { stateNow.blockWidthPx == blockWidthPx }
                ?.firstOrNull { it.text == blockText }
                ?.copy(textIndex = textIndex, blockIndex = blockIndex)
                ?: parseBlockContent(blockText, textIndex, blockIndex, ruler, style, blockWidthPx, spacePx)
            textIndex += blockText.length + 1
            blockContent
        }

        val nextState = stateNow.copy(text = text, blocks = blocks, blockWidthPx = blockWidthPx)

        setState {
            if (cursorTarget != nextState.cursor.textIndex) {
                nextState.moveCursorToIndex(cursorTarget - stateNow.cursor.textIndex, ruler, style, spacePx)
            } else nextState
        }
    }

    fun moveCursor(delta: Int, isSelection: Boolean) {
        val selectionCursor = provideSelectionCursor(isSelection)
        setState { stateNow.moveCursorToIndex(delta, ruler, style, spacePx).copy(selectionCursor = selectionCursor) }
    }

    fun targetCursor(delta: Int) {
        cursorTarget = stateNow.cursor.textIndex + delta
    }

    fun moveCursorLine(delta: Int, isSelection: Boolean) {
        val selectionCursor = provideSelectionCursor(isSelection)
        setState { it.moveCursorLine(delta, ruler, style, spacePx).copy(selectionCursor = selectionCursor) }
    }

    private fun provideSelectionCursor(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectionCursor == null) stateNow.cursor
        else stateNow.selectionCursor
    } else null
}

internal data class WriterState(
    val text: String = "",
    val blocks: List<WriterBlock> = listOf(WriterBlock.Empty),
    val cursor: WriterCursor = WriterCursor.Home,
    // val selection: Selection? = null,
    val blockWidthPx: Int = 0,
    val selectionCursor: WriterCursor? = null
) {
    val selection get() = when {
        selectionCursor == null -> null
        selectionCursor.textIndex > cursor.textIndex -> Selection(cursor, selectionCursor)
        else -> Selection(selectionCursor, cursor)
    }
}

internal data class WriterCursor(
    val textIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val chunkIndex: Int,
    val offsetX: Int,
    val preferredOffsetX: Int
) {
    companion object {
        val Home = WriterCursor(0, 0, 0, 0, 0, 0)
    }
}

@Stable
internal data class WriterBlock(
    val text: String,
    val chunks: List<WriterChunk>,
    val lines: List<WriterLine>,
    val textIndex: Int,
    val blockIndex: Int,
) {
    val endTextIndex get() = textIndex + text.length

    companion object {
        val Empty = WriterBlock("", listOf(), emptyList(), 0, 0)
    }
}

internal data class WriterChunk(
    val text: String,
    val textLayout: TextLayoutResult,
    val blockTextIndex: Int,
    val lineIndex: Int,
    val chunkIndex: Int,
    val offsetX: Int,
    val isContinued: Boolean,
) {
    val endBlockTextIndex get() = blockTextIndex + text.length
    val endOffsetX get() = offsetX + textLayout.size.width

    companion object {
        // val Empty = WriterChunk("")
    }
}

internal data class WriterLine(
    val blockTextIndex: Int,
    val lineIndex: Int,
    val length: Int,
    val chunkIndex: Int,
    val chunkCount: Int,
) {
    val endChunkIndex get() = chunkIndex + chunkCount
    val endBlockTextIndex get() = blockTextIndex + length
}

internal data class Selection(
    val start: WriterCursor,
    val end: WriterCursor
)

internal fun WriterState.moveCursorLine(
    delta: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): WriterState {
    require(abs(delta) == 1) { "line cursor delta must be 1 or -1" }
    val blocks = blocks
    val block = blocks[cursor.blockIndex]
    val newLineIndex = cursor.lineIndex + delta
    val textIndex = if (newLineIndex < 0) {
        if (cursor.blockIndex == 0) return copy(cursor = WriterCursor.Home)
        val block = blocks[cursor.blockIndex - 1]
        val line = block.lines.last()
        findNearestTextIndex(block.blockIndex, line.lineIndex, cursor.preferredOffsetX, ruler, style)
    } else if (newLineIndex >= block.lines.size) {
        if (cursor.blockIndex + 1 >= blocks.size) {
            text.length
        } else {
            findNearestTextIndex(cursor.blockIndex + 1, 0, cursor.preferredOffsetX, ruler, style)
        }
    } else {
        findNearestTextIndex(cursor.blockIndex, newLineIndex, cursor.preferredOffsetX, ruler, style)
    }
    return createCursorAtIndex(textIndex, ruler, style, spacePx, false)
}

internal fun WriterState.createCursorAtIndex(
    textIndex: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int,
    setPreferred: Boolean
): WriterState {
    val block = blocks.first { it.endTextIndex >= textIndex }
    val blockTextIndex = textIndex - block.textIndex
    val line = block.lines.firstOrNull { it.endBlockTextIndex >= blockTextIndex }
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
    return copy(
        cursor = WriterCursor(
            textIndex = textIndex,
            blockIndex = block.blockIndex,
            lineIndex = line?.lineIndex ?: 0,
            chunkIndex = chunk?.chunkIndex ?: 0,
            offsetX = offsetX,
            preferredOffsetX = if (setPreferred) offsetX else cursor.preferredOffsetX
        )
    )
}

internal fun WriterState.moveCursorToIndex(
    delta: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): WriterState {
    val index = (cursor.textIndex + delta).coerceIn(0, text.length)
    return createCursorAtIndex(index, ruler, style, spacePx, true)
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
    } ?: block.chunks.last { it.lineIndex == lineIndex }
    if (targetX >= chunk.endOffsetX) return chunk.endBlockTextIndex
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