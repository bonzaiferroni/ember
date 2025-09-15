package ponder.ember.app.ui

import androidx.compose.ui.text.TextRange

internal fun List<String>.removeRange(selection: Selection) =
    if (selection.isMultiBlock) {
        removeBodyRange(selection.start.bodyIndex, selection.end.bodyIndex)
    } else {
        removeContentRange(
            blockIndex = selection.start.blockIndex,
            indexStart = selection.start.contentIndex,
            indexEnd = selection.end.contentIndex
        )
    }

internal fun List<String>.removeBodyRange(indexStart: Int, indexEnd: Int) = joinToString("\n")
    .removeRange(indexStart, indexEnd)
    .split("\n")

internal fun List<String>.removeContentRange(
    blockIndex: Int,
    indexStart: Int,
    indexEnd: Int,
) = mapIndexed { index, content ->
    if (index != blockIndex) {
        content
    } else {
        content.removeRange(indexStart, indexEnd)
    }
}

internal fun List<String>.insertText(caret: Caret, text: String) =
    if (text.contains('\n')) insertBodyText(caret.bodyIndex, text)
    else insertContentText(caret.blockIndex, caret.contentIndex, text)

internal fun List<String>.insertBodyText(bodyIndex: Int, text: String): List<String> = buildString {
    this@insertBodyText.forEachIndexed { index, content ->
        append(content)
        if (index < this@insertBodyText.size - 1) {
            append('\n')
        }
    }
    insert(bodyIndex, text)
}.split("\n")

internal fun List<String>.insertContentText(blockIndex: Int, contentIndex: Int, text: String) =
    mapIndexed { index, content ->
        if (blockIndex != index) content
        else StringBuilder(content).insert(contentIndex, text).toString()
    }