package ponder.ember.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class OldWriterModel(
    val ruler: TextMeasurer,
    val style: TextStyle,
    val spacePx: Int,
    val onValueChange: (String) -> Unit,
    val blockParser: OldBlockParser = OldBlockParser(ruler, style, spacePx),
) {
    private val state = MutableStateFlow(OldWriterState())
    internal val stateNow get() = state.value
    val stateFlow: StateFlow<OldWriterState> = state

//    private val history: MutableList<WriterState> = mutableListOf()
//    private var lastHistory: Instant = Instant.DISTANT_PAST

    private fun setState(block: (OldWriterState) -> OldWriterState) {
        state.value = block(state.value)
    }

    fun updateContent(text: String, caretIndex: Int? = null, blockWidthPx: Int = stateNow.blockWidthPx) {
        if (text == stateNow.text && blockWidthPx == stateNow.blockWidthPx) return

        var textIndex = 0
        val blocks = text.split('\n').mapIndexed { blockIndex, blockText ->
            val blockContent = stateNow.blocks.takeIf { stateNow.blockWidthPx == blockWidthPx }
                ?.firstOrNull { it.text == blockText }
                ?.copy(textIndex = textIndex, blockIndex = blockIndex)
                ?: blockParser.buildBlockContent(blockText, textIndex, blockIndex, blockWidthPx)
            textIndex += blockText.length + 1
            blockContent
        }

        val nextState = stateNow.copy(text = text, blocks = blocks, blockWidthPx = blockWidthPx)

        setState {
            nextState.copy(
                caret = caretIndex?.let { nextState.createCaretAtIndex(
                    textIndex = it,
                    lineIndex = null,
                    ruler = ruler,
                    style = style,
                    spacePx = spacePx
                ) } ?: nextState.caret
            )
        }
        onValueChange(text)
    }

    fun moveCaret(delta: Int, isSelection: Boolean, lineIndex: Int? = null) {
        val textIndex = stateNow.caret.textIndex + delta
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = stateNow.createCaretAtIndex(textIndex, lineIndex, ruler, style, spacePx)
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

    fun setCaret(caret: OldCaret, selectCaret: OldCaret?) {
        setState { it.copy(caret = caret, selectCaret = selectCaret) }
    }

    private fun provideSelectionCaret(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectCaret == null) stateNow.caret
        else stateNow.selectCaret
    } else null
}

internal data class OldWriterState(
    val text: String = "",
    val blocks: List<OldWriterBlock> = listOf(OldWriterBlock.Empty),
    val caret: OldCaret = OldCaret.Home,
    // val selection: Selection? = null,
    val blockWidthPx: Int = 0,
    val selectCaret: OldCaret? = null
) {
    val selection get() = when {
        selectCaret == null -> null
        selectCaret.textIndex > caret.textIndex -> OldSelection(caret, selectCaret)
        else -> OldSelection(selectCaret, caret)
    }

    val selectedText get() = selection?.let { text.substring(it.start.textIndex, it.end.textIndex) }
}

internal data class OldCaret(
    val textIndex: Int,
    val blockTextIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val chunkIndex: Int,
    val offsetX: Int,
    val preferredOffsetX: Int
) {
    companion object {
        val Home = OldCaret(0, 0, 0, 0, 0, 0, 0)
    }
}

@Stable
internal data class OldWriterBlock(
    val text: String,
    val chunks: List<WriterChunk>,
    val lines: List<WriterLine>,
    val textIndex: Int,
    val blockIndex: Int,
) {
    val endTextIndex get() = textIndex + text.length

    companion object {
        val Empty = OldWriterBlock("", listOf(), emptyList(), 0, 0)
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

    companion object {
        val Empty = WriterLine(
            blockTextIndex = 0,
            length = 0,
            chunkIndex = 0,
            chunkCount = 0,
            lineIndex = 0
        )
    }
}

internal data class OldSelection(
    val start: OldCaret,
    val end: OldCaret
)
