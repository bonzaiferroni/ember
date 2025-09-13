package ponder.ember.app.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import pondui.ui.theme.Pond

@Composable
fun BlockField(
    text: String,
    style: TextStyle = Pond.typo.body,
    onValueChange: (String) -> Unit
) {
    val localColors = Pond.localColors
    BasicTextField(
        value = text,
        textStyle = style.copy(color = localColors.content),
        cursorBrush = SolidColor(localColors.content),
        onValueChange = onValueChange
    )
}