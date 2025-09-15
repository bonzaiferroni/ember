package ponder.ember.app.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
fun OldWriter(
    text: String = "",
    onValueChange: (String) -> Unit = { }
) {
    var blockWidthPx by remember { mutableIntStateOf(0) }
    val style = Pond.typo.body
    val ruler = rememberTextMeasurer()
    val spacePx = rememberSpacePx(style)
    val model = remember { OldWriterModel(ruler, style, spacePx.width, onValueChange) }
    val state by model.stateFlow.collectAsState()
    val caretIndex = state.caret.textIndex
    val clipBoard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    model.updateContent(text, blockWidthPx = blockWidthPx)

    val selection = state.selection
    val caret = state.caret
    val listState = rememberLazyListState()

    LaunchedEffect(caret.lineIndex, caret.blockIndex) {
        listState.animateScrollToItem(caret.blockIndex)
    }

    LazyColumn(
        gap = 1,
        state = listState,
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
                        val block = state.blocks[caret.blockIndex]
                        val line = block.lines[caret.lineIndex]
                        if (caret.blockTextIndex == line.blockTextIndex) {
                            if (line.lineIndex - 1 >= 0) {
                                model.moveCaret(0, false, caret.lineIndex - 1)
                            } else {
                                model.moveCaret(-1, event.isShiftPressed, null)
                            }
                        } else if (!event.isShiftPressed && selection != null) {
                            model.setCaret(selection.start, null)
                        } else {
                            model.moveCaret(-1, event.isShiftPressed, caret.lineIndex)
                        }
                    }
                    Key.DirectionRight -> {
                        val block = state.blocks[caret.blockIndex]
                        val line = block.lines[caret.lineIndex]
                        if (caret.blockTextIndex == line.endBlockTextIndex) {
                            if (block.lines.size > line.lineIndex + 1) {
                                model.moveCaret(0, false, caret.lineIndex + 1)
                            } else {
                                model.moveCaret(1, event.isShiftPressed, null)
                            }
                        } else if (!event.isShiftPressed && selection != null) {
                            model.setCaret(selection.end, null)
                        } else {
                            model.moveCaret(1, event.isShiftPressed, caret.lineIndex)
                        }
                    }
                    Key.Enter -> model.addTextAtCaret("\n")
                    Key.MoveEnd -> {
                        val block = state.blocks[caret.blockIndex]
                        val line = block.lines[caret.lineIndex]
                        if (caret.blockTextIndex == line.endBlockTextIndex) {
                            model.moveCaret(text.length - caretIndex, event.isShiftPressed)
                        } else {
                            model.moveCaret(line.endBlockTextIndex - caret.blockTextIndex, event.isShiftPressed, caret.lineIndex)
                        }
                    }
                    Key.MoveHome -> {
                        val block = state.blocks[caret.blockIndex]
                        val line = block.lines[caret.lineIndex]
                        if (caret.blockTextIndex == line.blockTextIndex) {
                            model.moveCaret(-caretIndex, event.isShiftPressed)
                        } else {
                            model.moveCaret(line.blockTextIndex - caret.blockTextIndex, event.isShiftPressed, caret.lineIndex)
                        }
                    }
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
//            .clickable(
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
//            ) { focusRequester.requestFocus(); println("focused") }
    ) {
        items(state.blocks, { it.blockIndex } ) { block ->
            val isCaretPresent = isFocused && caretIndex >= block.textIndex && caretIndex <= block.endTextIndex
            val isSelectionPresent = selection != null && selection.start.textIndex < block.endTextIndex
                    && selection.end.textIndex > block.textIndex
            OldWriterBlock(
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

