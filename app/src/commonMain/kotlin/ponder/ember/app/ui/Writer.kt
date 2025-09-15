package ponder.ember.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import kotlinx.collections.immutable.ImmutableList
import pondui.ui.controls.LazyColumn
import pondui.ui.theme.Pond

@Composable
fun Writer(
    content: WriterContent,
    onValueChange: (List<String>) -> Unit
) {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val style = Pond.typo.body
    var blockWidthPx by remember { mutableIntStateOf(0) }
    val model = remember { WriterModel(
        style = style,
        density = density,
        resolver = resolver,
        onValueChange = onValueChange
    ) }
    val state by model.stateFlow.collectAsState()
    val caretIndex = state.caret.textIndex
    var isFocused by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    model.updateContent(content.contents, blockWidthPx = blockWidthPx)

    LazyColumn(
        gap = 1,
        state = listState,
        modifier = Modifier.onGloballyPositioned { layout -> blockWidthPx = layout.size.width  }
            .fillMaxWidth()
    ) {
        items(state.blocks, { it.blockIndex } ) { block ->
            val isCaretPresent = isFocused && caretIndex >= block.textIndex && caretIndex <= block.endTextIndex
//            val isSelectionPresent = selection != null && selection.start.textIndex < block.endTextIndex
//                    && selection.end.textIndex > block.textIndex
            WriterBlock(
                block = block,
                caret = state.caret.takeIf { isCaretPresent },
//                selection = selection.takeIf { isSelectionPresent },
            )
        }
    }
}

@Stable
data class WriterContent(
    val contents: List<String>
)