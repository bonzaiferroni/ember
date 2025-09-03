package ponder.ember.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import pondui.ui.controls.LazyColumn
import pondui.ui.theme.Pond

@Composable
fun Writer(
    text: String,
    onWord: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    var blockWidthPx by remember { mutableIntStateOf(0) }
    val style = Pond.typo.body
    val ruler = rememberTextMeasurer()
    val spacePx = rememberSpacePx(style)
    val model = remember { WriterModel(ruler, style, spacePx.width) }
    val state by model.stateFlow.collectAsState()
    val cursorIndex = state.cursor.textIndex

    model.updateContent(text, blockWidthPx)
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    fun addCharacter(char: Char) {
        val pos = minOf(text.length, cursorIndex)
        model.targetCursor(1)
        onValueChange(text.insertAt(pos, char))
    }

    LazyColumn(
        gap = 1,
        modifier = Modifier.onGloballyPositioned { layout -> blockWidthPx = layout.size.width  }
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                focusRequester.requestFocus()

                var isConsumed = true

                when (event.key) {
                    Key.Backspace -> {
                        model.moveCursor(-1)
                        onValueChange(text.dropLast(1))
                    }
                    Key.DirectionLeft -> model.moveCursor(-1)
                    Key.DirectionRight -> model.moveCursor(1)
                    Key.Enter -> addCharacter('\n')
                    Key.MoveEnd -> model.moveCursor(text.length - cursorIndex)
                    Key.MoveHome -> model.moveCursor(-cursorIndex)
                    Key.DirectionUp -> model.moveCursorLine(-1)
                    Key.DirectionDown -> model.moveCursorLine(1)
                    else -> isConsumed = false
                }

                // ignore pure modifiers (Shift/Ctrl/Alt/Meta) and combos
                if (event.key in modifierKeys) { isConsumed = true }

                val char = event.utf16CodePoint.toChar()
                if (!isConsumed && !char.isISOControl()) {
                    addCharacter(char)
                    isConsumed = true
                }
                isConsumed
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusRequester.requestFocus(); println("focused") }
    ) {
        items(state.blocks) { block ->
            val isCursorPresent = isFocused && cursorIndex >= block.textIndex && cursorIndex <= block.endTextIndex
            WriterBlock(
                content = block,
                cursor = state.cursor.takeIf { isCursorPresent },
                spacePx = spacePx
            )
        }
    }
}



private val modifierKeys = setOf(
    Key.ShiftLeft,
    Key.ShiftRight,
    Key.CtrlLeft,
    Key.CtrlRight,
    Key.AltLeft,
    Key.AltRight,
    Key.MetaLeft,
    Key.MetaRight
)

fun String.insertAt(i: Int, ch: Char): String =
    StringBuilder(this).insert(i, ch).toString()