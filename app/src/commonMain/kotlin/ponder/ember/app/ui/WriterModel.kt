package ponder.ember.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class WriterModel(
    private val style: TextStyle,
    private val density: Density,
    private val resolver: FontFamily.Resolver,
    val onValueChange: (List<String>) -> Unit,
    val blockParser: BlockParser = BlockParser(
        style = style,
        density = density,
        resolver = resolver
    )
) {
    private val state = MutableStateFlow(WriterState())
    internal val stateNow get() = state.value
    val stateFlow: StateFlow<WriterState> = state

    private fun setState(block: (WriterState) -> WriterState) {
        state.value = block(state.value)
    }

    fun updateContents(contents: List<String>, caretIndex: Int? = null, blockWidthPx: Int = stateNow.blockWidthPx) {
        if (contents == stateNow.contents && blockWidthPx == stateNow.blockWidthPx) return

        var bodyIndex = 0
        val blocks = contents.mapIndexed { blockIndex, blockText ->
            val blockContent = stateNow.blocks.takeIf { stateNow.blockWidthPx == blockWidthPx }
                ?.firstOrNull { it.content == blockText }
                ?.copy(bodyIndex = bodyIndex, blockIndex = blockIndex)
                ?: blockParser.buildBlockContent(blockText, bodyIndex, blockIndex, blockWidthPx)
            bodyIndex += blockText.length + 1
            blockContent
        }

        val nextState = stateNow.copy(
            contents = contents,
            blocks = blocks,
            blockWidthPx = blockWidthPx,
            bodyLength = bodyIndex - 1
        )

        setState {
            nextState.copy(
                caret = caretIndex?.let { nextState.createCaretAtIndex(
                    bodyIndex = it,
                ) } ?: nextState.caret
            )
        }
        onValueChange(contents)
    }

    fun addTextAtCaret(text: String) {
        val initialContents = stateNow.selection?.let { stateNow.contents.removeRange(it) } ?: stateNow.contents
        val caret = stateNow.caret
        val contents = initialContents.insertText(caret, text)
        updateContents(contents, caret.bodyIndex + text.length)
    }

    fun cutSelectionText() {
        val selection = stateNow.selection ?: return
        val text = stateNow.contents.removeRange(selection)
        setState { state -> state.copy(selectCaret = null) }
        updateContents(text, selection.start.bodyIndex)
    }

    fun moveCaretHorizontal(delta: Int, isSelection: Boolean) {
        val newBodyIndex = (stateNow.caret.bodyIndex + delta).coerceIn(0, stateNow.bodyLength)
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = stateNow.createCaretAtIndex(newBodyIndex)
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }

    private fun provideSelectionCaret(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectCaret == null) stateNow.caret
        else stateNow.selectCaret
    } else null

    fun moveCaretEnd(isSelection: Boolean) {
        val block = stateNow.caretBlock ?: return
        val paragraph = block.paragraph ?: return
        val currentCaret = stateNow.caret
        val selectionCaret = provideSelectionCaret(isSelection)
        val lineEndOffsetX = paragraph.getLineRight(currentCaret.lineIndex)
        val bodyIndex = if (lineEndOffsetX > currentCaret.offsetX) {
            block.bodyIndex + paragraph.getLineEnd(currentCaret.lineIndex) - 1
        } else {
            stateNow.bodyLength - 1
        }
        val caret = stateNow.createCaretAtIndex(bodyIndex)
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }
}


internal data class WriterState(
    val contents: List<String> = emptyList(),
    val blockWidthPx: Int = 0,
    val blocks: List<WriterBlock> = listOf(WriterBlock.Empty),
    val caret: Caret = Caret.Home,
    val selectCaret: Caret? = null,
    val bodyLength: Int = 0,
) {
    val selection get() = when {
        selectCaret == null -> null
        selectCaret.bodyIndex > caret.bodyIndex -> Selection(caret, selectCaret)
        else -> Selection(selectCaret, caret)
    }

    val caretBlock get() = caret.blockIndex.takeIf { it < blocks.size}?.let { blocks[it]}
}

internal data class Selection(
    val start: Caret,
    val end: Caret
) {
    val isMultiBlock get() = start.blockIndex != end.blockIndex
}

@Stable
internal data class WriterBlock(
    val content: String,
    val paragraph: Paragraph?,
    val blockIndex: Int,
    val bodyIndex: Int,
) {
    val bodyIndexEnd get() = bodyIndex + content.length

    companion object {
        val Empty = WriterBlock("", null, 0, 0)
    }
}

internal data class Caret(
    val bodyIndex: Int,
    val contentIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val offsetX: Float,
    val preferredOffsetX: Float
) {
    companion object {
        val Home = Caret(0, 0, 0, 0, 0f, 0f)
    }
}