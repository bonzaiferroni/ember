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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal fun WriterBlock(
    block: WriterBlock,
    caret: Caret?,
) {
    val paragraph = block.paragraph ?: return

    Box {
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