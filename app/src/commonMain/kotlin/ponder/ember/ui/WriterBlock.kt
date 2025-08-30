package ponder.ember.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pondui.ui.behavior.magic
import pondui.ui.controls.FlowRow
import pondui.ui.controls.Section
import pondui.ui.controls.Text
import pondui.ui.controls.actionable
import pondui.ui.theme.Pond
import kotlin.random.Random

@Composable
fun WriterBlock(
    text: String,
    onValueChange: (String) -> Unit
) {
    val parts = remember(text) { text.split(' ') }
    val localColors = Pond.localColors
    val style = Pond.typo.body
    val spaceDp = rememberSpaceDp(style)
    val focusRequester = remember { FocusRequester() }

    Section(
        modifier = Modifier.widthIn(min = 20.dp)
            .actionable { focusRequester.requestFocus() }
    ) {
        Box() {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spaceDp)) {
                parts.forEachIndexed { index, part ->
                    if (index < parts.size - 1) {
                        val flipIndex = remember { (0..2).random() }
                        val flipDirection = remember { randomFlip() }
                        BasicText(
                            text = part,
                            style = style,
                            color = { localColors.content } ,
                            modifier = Modifier.magic(
                                rotationX = if (flipIndex == 0) 360 * flipDirection else 0,
                                rotationY = if (flipIndex == 1) 360 * flipDirection else 0,
                                rotationZ = if (flipIndex == 2) 360 * flipDirection else 0,
                                fade = false,
                                durationMillis = 800
                            ),
                        )
                    }
                }
                val lastPart = parts.last()
                BasicTextField(
                    value = lastPart,
                    textStyle = style.copy(color = localColors.content),
                    onValueChange = {
                        onValueChange("${text.dropLast(lastPart.length)}$it")
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(localColors.content),
                    modifier = Modifier.width(IntrinsicSize.Min)
                        .background(Color.White.copy(.1f))
                        .focusRequester(focusRequester)
                )
            }
        }
    }
}

@Composable
fun rememberSpaceDp(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(style) {
        val withSpace = measurer.measure("x x", style = style, softWrap = false)
        val noSpace   = measurer.measure("xx",        style = style, softWrap = false)
        val px = withSpace.size.width - noSpace.size.width
        with(density) { px.toDp() }
    }
}

fun randomFlip(): Int = 1 - 2 * Random.nextInt(2)
