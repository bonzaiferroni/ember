package ponder.ember.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

@Composable
internal fun WriterBlock(
    block: WriterBlock,
    caret: Caret?,
) {
    val paragraph = block.paragraph ?: return

    Box(
        modifier = Modifier.fillMaxWidth()
            .height(paragraph.height.dp)
            .drawBehind {
                paragraph.paint(drawContext.canvas)
            }
    )
}