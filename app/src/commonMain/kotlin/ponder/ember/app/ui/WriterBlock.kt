package ponder.ember.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.dp

@Composable
internal fun WriterBlock(
    block: TextBlock,
    caret: Caret?,
    selection: Selection?,
) {
    val paragraph = block.paragraph ?: return

    val selectionLines = selection?.let { selection ->
        block.lines.mapNotNull { line ->
            val lineBodyIndex = block.bodyIndex + line.contentIndex
            val lineBodyIndexEnd = block.bodyIndex + line.contentIndexEnd
            if (selection.start.bodyIndex > lineBodyIndexEnd || selection.end.bodyIndex < lineBodyIndex)
                return@mapNotNull null
            if (selection.start.blockIndex == block.blockIndex && selection.start.lineIndex > line.lineIndex)
                return@mapNotNull null
            if (selection.end.blockIndex == block.blockIndex && selection.end.lineIndex < line.lineIndex)
                return@mapNotNull null
            val startX = if (selection.start.bodyIndex > lineBodyIndex) selection.start.offsetX else line.left
            val endX = if (selection.end.bodyIndex < lineBodyIndexEnd) selection.end.offsetX
            else line.width
            val topLeft = Offset(
                x = startX,
                y = line.top
            )
            val size = Size(
                width = (endX - startX),
                height = line.height
            )
            Pair(topLeft, size)
        }
    }

    val selectionColor = Color.Cyan.copy(.3f)

    Box {
        selectionLines?.forEach { (topLeft, size) ->
            Box(
                modifier = Modifier.drawBehind {
                    drawRoundRect(selectionColor, topLeft, size, cornerRadius = CornerRadius(3f))
                }
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth()
                .height(paragraph.height.dp)
                .drawBehind {
                    paragraph.paint(drawContext.canvas)
                }
        )
        caret?.let {
            DrawCaret(caret, paragraph)
        }
    }
}

@Composable
internal fun DrawCaret(
    caret: Caret,
    paragraph: Paragraph,
) {
    val density = LocalDensity.current
    val caretAlpha = remember { Animatable(1f) }
    val height = paragraph.getLineHeight(caret.lineIndex)
    val offsetY = paragraph.getLineTop(caret.lineIndex)

    Box(
        modifier = Modifier.width(1.dp)
            .height(height.dp)
            .offset(x = caret.offsetX.dp, y = offsetY.dp)
            .graphicsLayer { this.alpha = caretAlpha.value }
            .background(Color.White)
    )

    LaunchedEffect(Unit) {
        val fadeMs = 320
        val holdMs = 90
        while (true) {
            // fade in
            caretAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fadeMs, easing = FastOutSlowInEasing)
            )
            // slight hold visible
            caretAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = holdMs, easing = LinearEasing)
            )
            // fade out
            caretAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = fadeMs, easing = FastOutSlowInEasing)
            )
            // slight hold hidden
            caretAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = holdMs, easing = LinearEasing)
            )
        }
    }
}