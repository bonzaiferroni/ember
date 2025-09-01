package ponder.ember.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.unit.dp
import pondui.ui.controls.LazyColumn

@Composable
fun Writer(
    text: String,
    onWord: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val blocks = remember(text) { text.split('\n') }
    val focusRequester = remember { FocusRequester() }
    var cursor by remember { mutableIntStateOf(text.length) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(text) {
        cursor = cursor.coerceIn(0, text.length)
    }

    fun moveCursor(delta: Int) {
        cursor += delta
    }

    fun addCharacter(char: Char) {
        val pos = minOf(text.length, cursor)
        onValueChange(text.insertAt(pos, char))
        moveCursor(1)
    }

    LazyColumn(
        gap = 1,
        modifier = Modifier.widthIn(min = 20.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                focusRequester.requestFocus()

                var isConsumed = true

                when (event.key) {
                    Key.Backspace -> {
                        moveCursor(-1)
                        onValueChange(text.dropLast(1))
                    }
                    Key.DirectionLeft -> moveCursor(-1)
                    Key.DirectionRight -> moveCursor(1)
                    Key.Enter -> addCharacter('\n')
                    Key.MoveEnd -> moveCursor(text.length - cursor)
                    Key.MoveHome -> moveCursor(-cursor)
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
        itemsIndexed(blocks) { index, block ->
            val blockStartIndex = (0 until index).sumOf { blocks[it].length + 1 }
            val blockEndIndex = blockStartIndex + block.length
            // println("c: $cursor, s: $blockStartIndex, e: $blockEndIndex, f: $isFocused")
            WriterBlock(
                text = block,
                cursor = cursor.takeIf {
                    isFocused && it >= blockStartIndex && it <= blockEndIndex
                }?.let { it - blockStartIndex },
                onWord = onWord,
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