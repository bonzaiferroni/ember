package ponder.ember.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.DpSize
import pondui.ui.theme.Pond

@Composable
fun WriterWord(
    word: String,
    textLayout: TextLayoutResult,
    size: DpSize,
    modifier: Modifier = Modifier
) {
    val localColors = Pond.localColors
    var textColor by remember(word) { mutableStateOf(addedWordColor) }
    val targetColor by animateColorAsState(textColor, animationSpec = tween(durationMillis = 10000))

    LaunchedEffect(word) {
        textColor = localColors.content
    }

    Box(
        modifier = modifier.size(size)
            .drawBehind {
                drawText(textLayout, targetColor)
            }
    )
}

@Composable
fun WriterWordAdv(
    word: String,
    textLayout: TextLayoutResult,
    ruler: TextMeasurer,
    style: TextStyle,
    cursor: Int?,
    size: DpSize,
    modifier: Modifier = Modifier
) {
    val localColors = Pond.localColors
    var textColor by remember(word) { mutableStateOf(addedWordColor) }
    val targetColor by animateColorAsState(textColor, animationSpec = tween(durationMillis = 10000))
    var lastWord by remember { mutableStateOf("")}
    var prefixLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var infixLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var suffixLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val animation = remember { Animatable(0f) }

    LaunchedEffect(word) {
        textColor = localColors.content

        if (cursor != null && cursor > 0 && word.length > lastWord.length) {
            val prefixIndex = cursor - 1
            println("$lastWord -> $word, p: $prefixIndex s: $cursor")
            animation.snapTo(0f)
            prefixLayout = ruler.measure(word.take(prefixIndex), style)
            infixLayout = ruler.measure(word.substring(prefixIndex, cursor))
            suffixLayout = cursor.takeIf { it < word.length }?.let { ruler.measure(word.substring(cursor)) }
            animation.animateTo(1f)
            prefixLayout = null
            infixLayout = null
            suffixLayout = null
        }
        lastWord = word
    }

    Box(
        modifier = modifier.size(size)
            .drawBehind {
                if (prefixLayout != null) {
                    val a = animation.value
                    prefixLayout?.let {
                        prefixLayout -> drawText(prefixLayout, targetColor)
                        infixLayout?.let { infixLayout ->
                            val scale = ((ANIM_SCALE - 1f) - (ANIM_SCALE - 1f) * a) + 1f
                            val prefixWidth = prefixLayout.size.width.toFloat()
//                            withTransform({ scale(scale, scale) }) {
//                                drawText(infixLayout, targetColor, topLeft = Offset(prefixWidth, 0f), alpha = a)
//                            }

                            suffixLayout?.let { suffixLayout ->
                                val infixWidth = infixLayout.size.width.toFloat()
                                val suffixX = prefixWidth + infixWidth * a
                                drawText(suffixLayout, targetColor, topLeft = Offset(suffixX, 0f))
                            }
                        }
                    }

                } else {
                    drawText(textLayout, targetColor)
                }

            }
    )
}

private val addedCharColor = Color(.4f, .8f, 1f)
private val addedWordColor = Color(1f, .6f, .8f)

fun String.commonPrefixIndex(other: String): Int {
    val limit = if (length < other.length) length else other.length
    var i = 0
    while (i < limit && this[i] == other[i]) {
        i++
    }
    return i
}

fun String.commonSuffixIndex(other: String): Int {
    val limit = if (length < other.length) length else other.length
    var i = 0
    while (i < limit && this[length - 1 - i] == other[other.length - 1 - i]) {
        i++
    }
    return length - i
}

private const val ANIM_SCALE = 2f