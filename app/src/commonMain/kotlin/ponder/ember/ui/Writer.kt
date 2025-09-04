package ponder.ember.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import pondui.ui.controls.LazyColumn
import pondui.ui.theme.Pond

@Composable
fun Writer(
    text: String = "",
    onValueChange: (String) -> Unit = { }
) {
    var blockWidthPx by remember { mutableIntStateOf(0) }
    val style = Pond.typo.body
    val ruler = rememberTextMeasurer()
    val spacePx = rememberSpacePx(style)
    val model = remember { WriterModel(ruler, style, spacePx.width) }
    val state by model.stateFlow.collectAsState()
    val caretIndex = state.caret.textIndex
    val clipBoard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(text, blockWidthPx) {
        model.updateContent(text, blockWidthPx = blockWidthPx)
    }

    LaunchedEffect(state.text) {
        if (state.text != text)
            onValueChange(state.text)
    }

    val selection = state.selection

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
                        if (selection == null) {
                            model.moveCaret(-1, true)
                        }
                        model.cutSelectionText()
                    }
                    Key.DirectionLeft -> {
                        if (!event.isShiftPressed && selection != null) {
                            model.setCaret(selection.start, null)
                        } else {
                            model.moveCaret(-1, event.isShiftPressed)
                        }
                    }
                    Key.DirectionRight -> {
                        if (!event.isShiftPressed && selection != null) {
                            model.setCaret(selection.end, null)
                        } else {
                            model.moveCaret(1, event.isShiftPressed)
                        }
                    }
                    Key.Enter -> model.addTextAtCaret("\n")
                    Key.MoveEnd -> model.moveCaret(text.length - caretIndex, event.isShiftPressed)
                    Key.MoveHome -> model.moveCaret(-caretIndex, event.isShiftPressed)
                    Key.DirectionUp -> model.moveCaretLine(-1, event.isShiftPressed)
                    Key.DirectionDown -> model.moveCaretLine(1, event.isShiftPressed)
                    Key.A -> {
                        if (event.isCtrlPressed) {
                            model.moveCaret(-caretIndex, false)
                            model.moveCaret(text.length, true)
                        } else isConsumed = false
                    }
                    Key.X -> {
                        if (event.isCtrlPressed) {
                            state.selectedText?.let { clipBoard.setText(AnnotatedString(it)) }
                            model.cutSelectionText()
                        } else isConsumed = false
                    }
                    Key.C -> {
                        if (event.isCtrlPressed) {
                            state.selectedText?.let { clipBoard.setText(AnnotatedString(it)) }
                        } else isConsumed = false
                    }
                    Key.V -> {
                        if (event.isCtrlPressed) {
                            val text = clipBoard.getText()?.text
                            if (text != null) model.addTextAtCaret(text)
                        } else isConsumed = false
                    }
                    else -> isConsumed = false
                }

                // ignore pure modifiers (Shift/Ctrl/Alt/Meta) and combos
                if (event.key in modifierKeys) { isConsumed = true }

                val char = event.utf16CodePoint.toChar()
                if (!isConsumed && !char.isISOControl()) {
                    model.addTextAtCaret(char.toString())
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
            val isCaretPresent = isFocused && caretIndex >= block.textIndex && caretIndex <= block.endTextIndex
            val isSelectionPresent = selection != null && selection.start.textIndex < block.endTextIndex
                    && selection.end.textIndex > block.textIndex
            WriterBlock(
                block = block,
                caret = state.caret.takeIf { isCaretPresent },
                selection = selection.takeIf { isSelectionPresent },
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

