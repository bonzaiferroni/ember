package ponder.ember.app.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

internal fun WriterState.setCaretAtIndex(
    textIndex: Int,
    lineIndex: Int?,
): Caret {
    // val index = (textIndex).coerceIn(0, text.length)
    // return createCaretAtIndex(index, lineIndex, ruler, style, spacePx, true)
    return Caret.Home
}