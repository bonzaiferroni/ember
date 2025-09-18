package ponder.ember.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class WriterModel(
    private val styles: StyleSet,
    private val density: Density,
    private val resolver: FontFamily.Resolver,
    val onValueChange: (WriterBody) -> Unit,
    val blockParser: BlockParser = BlockParser(
        styles = styles,
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
            bodyLength = bodyIndex - 1,
            selectCaret = null
        ).let { state ->
            state.copy(caret = caretIndex?.let { state.createCaretAtIndex(bodyIndex = it) } ?: state.caret)
        }

        setState { nextState }
        onValueChange(nextState)
    }

    fun addTextAtCaret(text: String) {
        val initialContents = stateNow.selection?.let { stateNow.contents.removeRange(it) } ?: stateNow.contents
        val caret = stateNow.selection?.start ?: stateNow.caret
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

    fun moveCaretEnd(isLineEnd: Boolean, isSelection: Boolean) {
        val block = stateNow.caretBlock ?: return
        val currentCaret = stateNow.caret
        val line = block.lines[currentCaret.lineIndex]
        val selectionCaret = provideSelectionCaret(isSelection)
        val bodyIndex = if (isLineEnd && line.right > currentCaret.offsetX) {
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

    fun moveCaretHome(isLineHome: Boolean, isSelection: Boolean) {
        val currentCaret = stateNow.caret
        val selectionCaret = provideSelectionCaret(isSelection)
        val caret = if (!isLineHome || currentCaret.offsetX == 0f) {
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
    override val contents: List<String> = emptyList(),
    override val blocks: List<TextBlock> = listOf(TextBlock.Empty),
    val blockWidthPx: Int = 0,
    val caret: Caret = Caret.Home,
    val selectCaret: Caret? = null,
    val bodyLength: Int = 0,
): WriterBody {
    val selection get() = when {
        selectCaret == null -> null
        selectCaret.bodyIndex > caret.bodyIndex -> Selection(caret, selectCaret)
        else -> Selection(selectCaret, caret)
    }

    val caretBlock get() = caret.blockIndex.takeIf { it < blocks.size}?.let { blocks[it]}

    val bodyText by lazy { contents.joinToString("\n") }

    val selectedText get() = selection?.let { bodyText.substring(it.start.bodyIndex, it.end.bodyIndex) }
}