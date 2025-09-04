package ponder.ember.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

internal fun parseBlockContent(
    text: String,
    textIndex: Int,
    blockIndex: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    blockWidthPx: Int,
    spacePx: Int,
): WriterBlock {
    if (text.isEmpty()) return WriterBlock(
        text = "",
        chunks = emptyList(),
        lines = emptyList(),
        textIndex = textIndex,
        blockIndex = blockIndex
    )

    val chunks = mutableListOf<WriterChunk>()
    val lines = mutableListOf<WriterLine>()
    var index = 0
    var offsetX = 0
    var lineTextIndex = 0
    var lineChunkCount = 0
    var startChunkIndex = 0

    fun finishLine(newLineIndex: Int) {
        lines.add(WriterLine(
            blockTextIndex = lineTextIndex,
            length = newLineIndex - lineTextIndex,
            chunkIndex = startChunkIndex,
            chunkCount = lineChunkCount,
            lineIndex = lines.size
        ))
        lineTextIndex = newLineIndex
        lineChunkCount = 0
        startChunkIndex = chunks.size
    }

    text.split(' ').forEach { word ->
        val textLayout = ruler.measure(word, style)
        if (textLayout.size.width > blockWidthPx) {
            offsetX = 0
            var subIndex = 0
            while (subIndex < word.length) {
                var endIndex = word.length
                while (endIndex > subIndex) {
                    val subWord = word.substring(subIndex, endIndex)
                    val subLayout = ruler.measure(subWord, style)
                    if (subLayout.size.width > blockWidthPx) {
                        endIndex--
                    } else {
                        val isContinued = subIndex + subWord.length < word.length
                        finishLine(index + subIndex)
                        val writerChunk = WriterChunk(
                            text = subWord,
                            textLayout = subLayout,
                            blockTextIndex = index + subIndex,
                            isContinued = isContinued,
                            offsetX = 0,
                            lineIndex = lines.size,
                            chunkIndex = chunks.size,
                        )
                        chunks.add(writerChunk)
                        lineChunkCount = 1
                        subIndex += subWord.length
                        if (!isContinued) offsetX += subLayout.size.width + spacePx
                    }
                }
            }
        } else {
            if (offsetX + textLayout.size.width > blockWidthPx) {
                offsetX = 0
                finishLine(index)
            }
            val writerChunk = WriterChunk(
                text = word,
                textLayout = textLayout,
                blockTextIndex = index,
                isContinued = false,
                offsetX = offsetX,
                lineIndex = lines.size,
                chunkIndex = chunks.size,
            )
            chunks.add(writerChunk)
            offsetX += textLayout.size.width + spacePx
        }
        index += word.length + 1
        lineChunkCount++
    }

    finishLine(index)

    return WriterBlock(
        text = text,
        chunks = chunks,
        lines = lines,
        textIndex = textIndex,
        blockIndex = blockIndex,
    )
}