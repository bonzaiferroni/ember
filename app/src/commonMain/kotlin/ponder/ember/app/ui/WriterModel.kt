package ponder.ember.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
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

    fun moveCaretVertical(delta: Int, isSelection: Boolean) {
        val currentCaret = stateNow.caret
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = if (delta < 0) {
            if (currentCaret.lineIndex == 0) {
                if (currentCaret.blockIndex == 0) return
                val block = stateNow.blocks[currentCaret.blockIndex - 1]
                val paragraph = block.paragraph ?: return
                val contentIndex = paragraph.getOffsetForPosition(Offset(currentCaret.preferredOffsetX, paragraph.height))
                stateNow.createCaretAtIndex(block.bodyIndex + contentIndex, currentCaret.preferredOffsetX)
            } else {
                val block = stateNow.blocks[currentCaret.blockIndex]
                val paragraph = block.paragraph ?: return
                val height = paragraph.getLineTop(currentCaret.lineIndex)
                val contentIndex = paragraph.getOffsetForPosition(Offset(currentCaret.preferredOffsetX, height))
                stateNow.createCaretAtIndex(block.bodyIndex + contentIndex, currentCaret.preferredOffsetX)
            }
        } else {
            val block = stateNow.blocks[currentCaret.blockIndex]
            val paragraph = block.paragraph ?: return
            if (currentCaret.lineIndex >= paragraph.lineCount - 1) {
                if (currentCaret.blockIndex >= stateNow.blocks.size - 1) return
                val nextBlock = stateNow.blocks[currentCaret.blockIndex + 1]
                val nextParagraph = nextBlock.paragraph ?: return
                val contentIndex = nextParagraph.getOffsetForPosition(Offset(currentCaret.preferredOffsetX, 0f))
                stateNow.createCaretAtIndex(nextBlock.bodyIndex + contentIndex, currentCaret.preferredOffsetX)
            } else {
                val height = paragraph.getLineBottom(currentCaret.lineIndex)
                val contentIndex = paragraph.getOffsetForPosition(Offset(currentCaret.preferredOffsetX, height))
                stateNow.createCaretAtIndex(block.bodyIndex + contentIndex, currentCaret.preferredOffsetX)
            }
        }
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }

    private fun provideSelectionCaret(isSelection: Boolean) = if (isSelection) {
        if (stateNow.selectCaret == null) stateNow.caret
        else stateNow.selectCaret
    } else null

    fun moveCaretEnd(isSelection: Boolean) {
        val block = stateNow.caretBlock ?: return
        val currentCaret = stateNow.caret
        val line = block.lines[currentCaret.lineIndex]
        val selectionCaret = provideSelectionCaret(isSelection)
        val bodyIndex = if (line.right > currentCaret.offsetX) {
            if (line.isLast) {
                line.bodyIndexEnd
            } else {
                line.bodyIndexEnd - 1
            }
        } else {
            stateNow.bodyLength
        }
        val caret = stateNow.createCaretAtIndex(bodyIndex)
        setState { it.copy(caret = caret, selectCaret = selectionCaret) }
    }

    fun moveCaretHome(isSelection: Boolean) {
        val currentCaret = stateNow.caret
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = if (currentCaret.offsetX == 0f) {
            Caret.Home
        } else {
            val block = stateNow.caretBlock ?: return
            val paragraph = block.paragraph ?: return
            val bodyIndex = block.bodyIndex + paragraph.getLineStart(currentCaret.lineIndex)
            stateNow.createCaretAtIndex(bodyIndex)
        }
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

    val bodyText by lazy { contents.joinToString("\n") }

    val selectedText = selection?.let { bodyText.substring(it.start.bodyIndex, it.end.bodyIndex) }
}

internal data class Selection(
    val start: Caret,
    val end: Caret
) {
    val isMultiBlock get() = start.blockIndex != end.blockIndex
    val length get() = end.bodyIndex - start.bodyIndex
}

@Stable
internal data class WriterBlock(
    val content: String,
    val paragraph: Paragraph?,
    val lines: List<WriterLine>,
    val blockIndex: Int,
    val bodyIndex: Int,
) {
    val bodyIndexEnd get() = bodyIndex + content.length

    companion object {
        val Empty = WriterBlock("", null, emptyList(), 0, 0)
    }
}

internal data class WriterLine(
    val lineIndex: Int,
    val bodyIndex: Int,
    val contentIndex: Int,
    val length: Int,
    val width: Float,
    val height: Float,
    val left: Float,
    val top: Float,
    val isLast: Boolean,
    val isFirst: Boolean,
) {
    val contentIndexEnd get() = contentIndex + length
    val bodyIndexEnd get() = bodyIndex + length
    val right get() = left + width
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