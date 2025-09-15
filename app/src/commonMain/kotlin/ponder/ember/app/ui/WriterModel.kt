package ponder.ember.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextRange
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
                ?.firstOrNull { it.text == blockText }
                ?.copy(bodyRange = TextRange(bodyIndex, blockText.length), blockIndex = blockIndex)
                ?: blockParser.buildBlockContent(blockText, bodyIndex, blockIndex, blockWidthPx)
            bodyIndex += blockText.length + 1
            blockContent
        }

        val nextState = stateNow.copy(contents = contents, blocks = blocks, blockWidthPx = blockWidthPx)

        setState {
            nextState.copy(
                caret = caretIndex?.let { nextState.setCaretAtIndex(
                    bodyIndex = it,
                ) } ?: nextState.caret
            )
        }
        onValueChange(contents)
    }

    fun addTextAtCaret(value: String) {
        val caret = stateNow.caret
        val contents = stateNow.contents.mapIndexed { index, content ->
            if (index != caret.blockIndex) content
            else content.insertAt(caret.contentIndex, value)
        }
        updateContents(contents, caret.bodyIndex + value.length)
    }
}

internal data class WriterState(
    val contents: List<String> = emptyList(),
    val blockWidthPx: Int = 0,
    val blocks: List<WriterBlock> = listOf(WriterBlock.Empty),
    val caret: Caret = Caret.Home
)

@Stable
internal data class WriterBlock(
    val text: String,
    val paragraph: Paragraph?,
    val blockIndex: Int,
    val bodyRange: TextRange,
) {
    companion object {
        val Empty = WriterBlock("", null, 0, TextRange(0, 0))
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