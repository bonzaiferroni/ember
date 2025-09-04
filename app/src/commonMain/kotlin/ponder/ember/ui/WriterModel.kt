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

    fun updateContent(text: String, cursorIndex: Int? = null, blockWidthPx: Int = stateNow.blockWidthPx) {
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
            nextState.copy(
                cursor = cursorIndex?.let { nextState.setCursorAtIndex(it, ruler, style, spacePx) } ?: nextState.cursor
            )
        }
    }

    fun moveCursor(delta: Int, isSelection: Boolean) {
        val textIndex = stateNow.cursor.textIndex + delta
        val selectionCursor = provideSelectionCursor(isSelection)
        val cursor = stateNow.setCursorAtIndex(textIndex, ruler, style, spacePx)
        setState { it.copy(cursor = cursor, selectionCursor = selectionCursor) }
    }

    fun moveCursorLine(lineDelta: Int, isSelection: Boolean) {
        val selectionCursor = provideSelectionCursor(isSelection)
        val cursor = stateNow.moveCursorLine(lineDelta, ruler, style, spacePx)
        setState { it.copy(cursor = cursor, selectionCursor = selectionCursor) }
    }

    fun addTextAtCursor(value: String) {
        val selection = stateNow.selection
        val text = selection?.let {
            setState { state -> state.copy(selectionCursor = null) }
            stateNow.text.removeRange(it.start.textIndex, it.end.textIndex)
        } ?: stateNow.text
        val cursorIndex = selection?.start?.textIndex ?: stateNow.cursor.textIndex
        val modifiedText = text.insertAt(cursorIndex, value)
        updateContent(modifiedText, cursorIndex + value.length)
    }

    fun cutSelectionText() {
        val selection = stateNow.selection ?: return
        val text = stateNow.text.removeRange(selection.start.textIndex, selection.end.textIndex)
        setState { state -> state.copy(selectionCursor = null) }
        updateContent(text, selection.start.textIndex)
    }

    fun setCursor(cursor: CursorState, selectionCursor: CursorState?) {
        setState { it.copy(cursor = cursor, selectionCursor = selectionCursor) }
    }

    private fun provideSelectionCursor(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectionCursor == null) stateNow.cursor
        else stateNow.selectionCursor
    } else null
}

internal data class WriterState(
    val text: String = "",
    val blocks: List<WriterBlock> = listOf(WriterBlock.Empty),
    val cursor: CursorState = CursorState.Home,
    // val selection: Selection? = null,
    val blockWidthPx: Int = 0,
    val selectionCursor: CursorState? = null
) {
    val selection get() = when {
        selectionCursor == null -> null
        selectionCursor.textIndex > cursor.textIndex -> Selection(cursor, selectionCursor)
        else -> Selection(selectionCursor, cursor)
    }

    val selectedText get() = selection?.let { text.substring(it.start.textIndex, it.end.textIndex) }
}

internal data class CursorState(
    val textIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val chunkIndex: Int,
    val offsetX: Int,
    val preferredOffsetX: Int
) {
    companion object {
        val Home = CursorState(0, 0, 0, 0, 0, 0)
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
    val start: CursorState,
    val end: CursorState
)

internal fun WriterState.moveCursorLine(
    delta: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): CursorState {
    require(abs(delta) == 1) { "line cursor delta must be 1 or -1" }
    val blocks = blocks
    val block = blocks[cursor.blockIndex]
    val newLineIndex = cursor.lineIndex + delta
    val textIndex = if (newLineIndex < 0) {
        if (cursor.blockIndex == 0) return CursorState.Home
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
): CursorState {
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
    return CursorState(
        textIndex = textIndex,
        blockIndex = block.blockIndex,
        lineIndex = line?.lineIndex ?: 0,
        chunkIndex = chunk?.chunkIndex ?: 0,
        offsetX = offsetX,
        preferredOffsetX = if (setPreferred) offsetX else cursor.preferredOffsetX
    )
}

internal fun WriterState.setCursorAtIndex(
    textIndex: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): CursorState {
    val index = (textIndex).coerceIn(0, text.length)
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