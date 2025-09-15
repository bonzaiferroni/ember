package ponder.ember.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
internal fun OldWriterBlock(
    block: OldWriterBlock,
    caret: OldCaret?,
    selection: Selection?,
    spacePx: IntSize,
) {
    val density = LocalDensity.current
    val spaceDp = with(density) { DpSize(spacePx.width.toDp(), spacePx.height.toDp()) }
    val lines = block.lines
    val chunks = block.chunks
    val lineSpaceDp = 1.dp
    val lineSpacePx = with(density) { lineSpaceDp.toPx() }

    val selectionLines = selection?.let { selection ->
        lines.mapNotNull { line ->
            val lineTextIndex = block.textIndex + line.blockTextIndex
            val lineEndTextIndex = block.textIndex + line.endBlockTextIndex
            if (selection.start.textIndex > lineEndTextIndex || selection.end.textIndex < lineTextIndex)
                return@mapNotNull null
            if (selection.start.blockIndex == block.blockIndex && selection.start.lineIndex > line.lineIndex)
                return@mapNotNull null
            if (selection.end.blockIndex == block.blockIndex && selection.end.lineIndex < line.lineIndex)
                return@mapNotNull null
            val startX = if (selection.start.textIndex > lineTextIndex) selection.start.offsetX else 0
            val endX = if (selection.end.textIndex < lineEndTextIndex) selection.end.offsetX
            else chunks.last { it.lineIndex == line.lineIndex }.endOffsetX + spacePx.width
            val topLeft = Offset(
                x = startX.toFloat(),
                y = ((spacePx.height + lineSpacePx) * line.lineIndex)
            )
            val size = Size(
                width = (endX - startX).toFloat(),
                height = spacePx.height.toFloat()
            )
            Pair(topLeft, size)
        }
    }

    val selectionColor = Color.Cyan.copy(.3f)

    Box(
        modifier = Modifier.fillMaxWidth()
            .height(maxOf((spaceDp.height + lineSpaceDp) * lines.size, spaceDp.height))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        for (change in event.changes) {
                            when {
                                change.changedToDown() -> {
                                    // val cursorIndex = findCursorIndex(block, )
                                }
                                change.changedToUp() -> {
                                    println("Mouse up at ${change.position}")
                                }
                                change.pressed -> {
                                    println("Mouse held at ${change.position}")
                                }
                            }
                        }

                        when (event.type) {
                            PointerEventType.Enter,
                            PointerEventType.Move -> {
                                val pos = event.changes.first().position
                                // println("Pointer at: $pos")
                            }
                            // PointerEventType.Exit -> println("Pointer left")
                        }
                    }
                }
            }
    ) {
        selectionLines?.forEach { (topLeft, size) ->
            Box(
                modifier = Modifier.drawBehind {
                        drawRoundRect(selectionColor, topLeft, size, cornerRadius = CornerRadius(3f))
                    }
            )
        }

        chunks.forEachIndexed { chunkIndex, chunk ->
            val text = chunk.text;
            val textLayout = chunk.textLayout
            val flipIndex = remember(text) { (0..2).random() }
            val flipDirection = remember(text) { 1.randomFlip() }
            val offsetXDp = with(density) { chunk.offsetX.toDp() }
            val offsetYDp = (spaceDp.height + lineSpaceDp) * chunk.lineIndex

            val size = with(density) { textLayout.size.let { DpSize(it.width.toDp(), it.height.toDp()) } }

            val width by animateDpAsState(size.width)

            WriterWord(
                word = text,
                isCaretBlock = caret != null,
                textLayout = textLayout,
                size = DpSize(minOf(size.width, width), size.height),
                modifier = Modifier.offset(offsetXDp, offsetYDp)
//                    .ifTrue(chunkIndex < chunks.size - 1 && caret != null) {
//                        magic(
//                            rotationX = if (flipIndex == 0) 360 * flipDirection else 0,
//                            rotationY = if (flipIndex == 1) 360 * flipDirection else 0,
//                            rotationZ = if (flipIndex == 2) 360 * flipDirection else 0,
//                            fade = false,
//                            durationMillis = 800
//                        )
//                    }
                    .padding(end = spaceDp.width)
            )
        }
        caret?.let {
            val offsetX = with(density) { it.offsetX.toDp() }
            val offsetY = (spaceDp.height + lineSpaceDp) * caret.lineIndex
            // println("offsetX: $offsetX textIndex: ${it.textIndex}")
            DrawCaret(offsetX, offsetY, spaceDp)
        }
    }
}

@Composable
internal fun DrawCaret(
    offsetX: Dp,
    offsetY: Dp,
    spaceDp: DpSize,
) {
    val caretAlpha = remember { Animatable(1f) }

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

    Box(
        modifier = Modifier.width(1.dp)
            .height(spaceDp.height)
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer { this.alpha = caretAlpha.value }
            .background(Color.White)
    )
}

@Composable
fun rememberSpacePx(style: TextStyle): IntSize {
    val measurer = rememberTextMeasurer()

    return remember(style) {
        val withSpace = measurer.measure("X X", style = style, softWrap = false)
        val noSpace = measurer.measure("XX", style = style, softWrap = false)
        IntSize(withSpace.size.width - noSpace.size.width, withSpace.size.height)
    }
}

@Composable
fun rememberSpaceDp2(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(style) {
        val spaceWidth = measurer.measure(" ", style = style, softWrap = false).size.width
        with(density) { spaceWidth.toDp() }
    }
}

fun Int.randomFlip(): Int = this * (1 - 2 * Random.nextInt(2))

//                val lastPart = parts.last()
//                val lastWordSize = remember(lastPart) {
//                    with(density) { ruler.measure(lastPart, style = style, softWrap = false).size.let { DpSize(it.width.toDp(), it.height.toDp()) } }
//                }
//                Box(
//                    modifier = Modifier.size(if (lastWordSize.width > 0.dp) lastWordSize else spaceDp)
//                ) {
//                    val charIndexStart = characterIndex
//                    val charIndexEnd = characterIndex + lastPart.length
//                    characterIndex += lastPart.length
//                    if (cursorPosition >= charIndexStart) {
//                        val beforeCursorSize = with(density) {
//                            ruler.measure(lastPart.take(cursorPosition - charIndexStart), style = style, softWrap = false).size.let {
//                                DpSize(it.width.toDp(), it.width.toDp())
//                            }
//                        }
//                        Box(
//                            modifier = Modifier.width(1.dp)
//                                .height(spaceDp.height)
//                                .offset(x = beforeCursorSize.width)
//                                .background(Color.White)
//                        )
//                    }
//
//                    lastPart.forEachIndexed { index, char ->
//                        val beforePartWidthDp = remember(char) {
//                            val beforePart = lastPart.take(index)
//                            val beforePartSize = ruler.measure(beforePart, style = style, softWrap = false)
//                            with(density) { beforePartSize.size.width.toDp() }
//                        }
//                        val y = remember(char) { if(addedChar) (20.randomFlip()).dp else 0.dp }
//                        var textColor by remember(char) { mutableStateOf(if (addedChar) addedCharColor else addedWordColor ) }
//                        val targetColor by animateColorAsState(textColor, animationSpec = tween(durationMillis = 400))
//                        LaunchedEffect(char) {
//                            textColor = addedWordColor
//                        }
//                        BasicText(
//                            text = "$char",
//                            style = style,
//                            color = { targetColor },
//                            modifier = Modifier.offset(x = beforePartWidthDp)
//                                .magic(
//                                    offsetX = 0.dp,
//                                    offsetY = y,
//                                    scale = if (addedChar) 2f else 1f,
//                                    fade = addedChar,
//                                    durationMillis = 200
//                                )
//                        )
//                    }
//                }