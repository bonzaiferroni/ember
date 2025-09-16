package ponder.ember.app.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import pondui.ui.controls.LazyColumn
import pondui.ui.theme.Pond

@Composable
fun Writer(
    content: WriterContent,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    styles: StyleSet? = null
) {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val clipBoard = LocalClipboardManager.current
    var blockWidthPx by remember { mutableIntStateOf(0) }
    val typography = Pond.typo
    val colors = Pond.colors
    val styles = styles ?: remember {
        StyleSet(
            paragraph = typography.body,
            h1 = typography.h1,
            h2 = typography.h2,
            h3 = typography.h3,
            h4 = typography.h4,
            h5 = typography.h5,
            symbolColor = colors.selection
        )
    }
    val model = remember { WriterModel(
        styles = styles,
        density = density,
        resolver = resolver,
        onValueChange = onValueChange
    ) }
    val state by model.stateFlow.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    model.updateContents(content.contents, blockWidthPx = blockWidthPx)

    val caret = state.caret
    val selection = state.selection

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(caret.lineIndex, caret.blockIndex) {
        listState.animateScrollToItem(caret.blockIndex)
    }

    LazyColumn(
        gap = 1,
        state = listState,
        modifier = modifier.onGloballyPositioned { layout -> blockWidthPx = layout.size.width  }
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
                            model.moveCaretHorizontal(-1, true)
                        }
                        model.cutSelectionText()
                    }
                    Key.DirectionLeft -> {
                        model.moveCaretHorizontal(-1, event.isShiftPressed)
                    }
                    Key.DirectionRight -> {
                        model.moveCaretHorizontal(1, event.isShiftPressed)
                    }
                    Key.Enter -> model.addTextAtCaret("\n")
                    Key.MoveEnd -> {
                        model.moveCaretEnd(true, event.isShiftPressed)
                    }
                    Key.MoveHome -> {
                        model.moveCaretHome(true, event.isShiftPressed)
                    }
                    Key.DirectionUp -> model.moveCaretVertical(-1, event.isShiftPressed)
                    Key.DirectionDown -> model.moveCaretVertical(1, event.isShiftPressed)
                    Key.A -> {
                        if (event.isCtrlPressed) {
                            model.moveCaretHorizontal(-caret.bodyIndex, false)
                            model.moveCaretHorizontal(state.bodyLength, true)
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
    ) {
        items(state.blocks, { it.blockIndex } ) { block ->
            val isCaretPresent = isFocused && caret.bodyIndex >= block.bodyIndex && caret.bodyIndex <= block.bodyIndexEnd
            val isSelectionPresent = selection != null && selection.start.bodyIndex < block.bodyIndexEnd
                    && selection.end.bodyIndex > block.bodyIndex
            WriterBlock(
                block = block,
                caret = state.caret.takeIf { isCaretPresent },
                selection = selection.takeIf { isSelectionPresent },
            )
        }
    }
}

@Stable
data class WriterContent(
    val contents: List<String>
)

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