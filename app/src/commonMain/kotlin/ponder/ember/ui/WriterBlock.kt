package ponder.ember.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import pondui.ui.behavior.ifTrue
import pondui.ui.behavior.magic
import pondui.ui.theme.Pond
import kotlin.random.Random

@Composable
fun WriterBlock(
    text: String,
    cursor: Int?,
    onWord: (String) -> Unit,
) {
    val words = remember(text) { text.split(' ') }
    val style = Pond.typo.body
    val spaceDp = rememberSpaceDp1(style)
    val ruler = rememberTextMeasurer()
    val density = LocalDensity.current

    FlowRow(
        modifier = Modifier
    ) {
        words.forEachIndexed { index, word ->
            val flipIndex = remember(word) { (0..2).random() }
            val flipDirection = remember(word) { 1.randomFlip() }
            Box(
                modifier = Modifier.padding(end = spaceDp.width)
            ) {
                val localCursor = cursor?.let {
                    val wordStartIndex = (0 until index).sumOf { words[it].length + 1 }
                    val wordEndIndex = wordStartIndex + word.length
                    if (cursor >= wordStartIndex && cursor <= wordEndIndex) {
                        val size = with(density) {
                            ruler.measure(
                                word.take(cursor - wordStartIndex),
                                style = style,
                                softWrap = false
                            ).size.let {
                                DpSize(it.width.toDp(), it.height.toDp())
                            }
                        }
                        WriterCursor(size.width, size.height)
                        cursor - wordStartIndex
                    } else null
                }

                val textLayout = ruler.measure(word, style)
                val size = with(density) { textLayout.size.let { DpSize(it.width.toDp(), it.height.toDp()) } }

                val width by animateDpAsState(size.width)

                WriterWord(
                    word = word,
                    textLayout = textLayout,
                    size = DpSize(minOf(size.width, width), size.height),
                    modifier = Modifier.ifTrue(index < words.size - 1) {
                        magic(
                            rotationX = if (flipIndex == 0) 360 * flipDirection else 0,
                            rotationY = if (flipIndex == 1) 360 * flipDirection else 0,
                            rotationZ = if (flipIndex == 2) 360 * flipDirection else 0,
                            fade = false,
                            durationMillis = 800
                        )
                    }
                )

//                BasicText(
//                    text = word,
//                    style = style,
//                    color = { targetColor },
//                )
            }
        }
    }
}

@Composable
fun WriterCursor(
    offsetX: Dp,
    height: Dp,
) {
    val cursorAlpha = remember { Animatable(cursorAlphaCache) }

    LaunchedEffect(cursorAlpha) {
        cursorAlphaCache = cursorAlpha.value
    }

    LaunchedEffect(Unit) {
        val fadeMs = 320
        val holdMs = 90
        while (true) {
            // fade in
            cursorAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fadeMs, easing = FastOutSlowInEasing)
            )
            // slight hold visible
            cursorAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = holdMs, easing = LinearEasing)
            )
            // fade out
            cursorAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = fadeMs, easing = FastOutSlowInEasing)
            )
            // slight hold hidden
            cursorAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = holdMs, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier.width(1.dp)
            .height(height)
            .offset(x = offsetX)
            .graphicsLayer { this.alpha = cursorAlpha.value }
            .background(Color.White)
    )
}

private var cursorAlphaCache = 0f

@Composable
fun rememberSpaceDp1(style: TextStyle): DpSize {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(style) {
        val withSpace = measurer.measure("X X", style = style, softWrap = false)
        val noSpace = measurer.measure("XX", style = style, softWrap = false)
        val px = withSpace.size.width - noSpace.size.width
        with(density) { DpSize(px.toDp(), withSpace.size.height.toDp()) }
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