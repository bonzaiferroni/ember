package ponder.ember.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlin.math.abs

internal class WriterModel(
    val ruler: TextMeasurer,
    val style: TextStyle,
    val spacePx: Int,
) {
    private val state = MutableStateFlow(WriterState())
    internal val stateNow get() = state.value
    val stateFlow: StateFlow<WriterState> = state

//    private val history: MutableList<WriterState> = mutableListOf()
//    private var lastHistory: Instant = Instant.DISTANT_PAST

    private fun setState(block: (WriterState) -> WriterState) {
        state.value = block(state.value)
    }

    fun updateContent(text: String, caretIndex: Int? = null, blockWidthPx: Int = stateNow.blockWidthPx) {
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
                caret = caretIndex?.let { nextState.setCaretAtIndex(
                    textIndex = it,
                    lineIndex = null,
                    ruler = ruler,
                    style = style,
                    spacePx = spacePx
                ) } ?: nextState.caret
            )
        }
    }

    fun moveCaret(delta: Int, isSelection: Boolean, lineIndex: Int? = null) {
        val textIndex = stateNow.caret.textIndex + delta
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = stateNow.setCaretAtIndex(textIndex, lineIndex, ruler, style, spacePx)
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }

    fun moveCaretLine(lineDelta: Int, isSelection: Boolean) {
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = stateNow.moveCaretLine(lineDelta, ruler, style, spacePx)
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }

    fun addTextAtCaret(value: String) {
        val selection = stateNow.selection
        val text = selection?.let {
            setState { state -> state.copy(selectCaret = null) }
            stateNow.text.removeRange(it.start.textIndex, it.end.textIndex)
        } ?: stateNow.text
        val caretIndex = selection?.start?.textIndex ?: stateNow.caret.textIndex
        val modifiedText = text.insertAt(caretIndex, value)
        updateContent(modifiedText, caretIndex + value.length)
    }

    fun cutSelectionText() {
        val selection = stateNow.selection ?: return
        val text = stateNow.text.removeRange(selection.start.textIndex, selection.end.textIndex)
        setState { state -> state.copy(selectCaret = null) }
        updateContent(text, selection.start.textIndex)
    }

    fun setCaret(caret: Caret, selectCaret: Caret?) {
        setState { it.copy(caret = caret, selectCaret = selectCaret) }
    }

    private fun provideSelectionCaret(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectCaret == null) stateNow.caret
        else stateNow.selectCaret
    } else null
}

internal data class WriterState(
    val text: String = "",
    val blocks: List<WriterBlock> = listOf(WriterBlock.Empty),
    val caret: Caret = Caret.Home,
    // val selection: Selection? = null,
    val blockWidthPx: Int = 0,
    val selectCaret: Caret? = null
) {
    val selection get() = when {
        selectCaret == null -> null
        selectCaret.textIndex > caret.textIndex -> Selection(caret, selectCaret)
        else -> Selection(selectCaret, caret)
    }

    val selectedText get() = selection?.let { text.substring(it.start.textIndex, it.end.textIndex) }
}

internal data class Caret(
    val textIndex: Int,
    val blockTextIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val chunkIndex: Int,
    val offsetX: Int,
    val preferredOffsetX: Int
) {
    companion object {
        val Home = Caret(0, 0, 0, 0, 0, 0, 0)
    }
}

internal data class WriterSnapshot(
    val text: String,
    val caret: Caret,
    val selectCaret: Caret
)

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
    val start: Caret,
    val end: Caret
)

internal fun WriterState.moveCaretLine(
    delta: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    spacePx: Int
): Caret {
    require(abs(delta) == 1) { "line caret delta must be 1 or -1" }
    val blocks = blocks
    val block = blocks[caret.blockIndex]
    val newLineIndex = caret.lineIndex + delta
    val textIndex = if (newLineIndex < 0) {
        if (caret.blockIndex == 0) return Caret.Home
        val block = blocks[caret.blockIndex - 1]
        val line = block.lines.last()
        findNearestTextIndex(block.blockIndex, line.lineIndex, caret.preferredOffsetX, ruler, style)
    } else if (newLineIndex >= block.lines.size) {
        if (caret.blockIndex + 1 >= blocks.size) {
            text.length
        } else {
            findNearestTextIndex(caret.blockIndex + 1, 0, caret.preferredOffsetX, ruler, style)
        }
    } else {
        findNearestTextIndex(caret.blockIndex, newLineIndex, caret.preferredOffsetX, ruler, style)
    }
    return createCaretAtIndex(textIndex, null, ruler, style, spacePx, false)
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
    val line = lineIndex?.let { block.lines[it] } ?: block.lines.firstOrNull { it.endBlockTextIndex >= blockTextIndex }
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