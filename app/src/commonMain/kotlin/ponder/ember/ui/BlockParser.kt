package ponder.ember.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

internal class BlockParser(
    private val ruler: TextMeasurer,
    private val style: TextStyle,
    private val spacePx: Int,
) {
    private val chunks = mutableListOf<WriterChunk>()
    private val lines = mutableListOf<WriterLine>()
    private var text: String = ""
    private var offsetX = 0
    private var lineTextIndex = 0
    private var lineChunkCount = 0
    private var startChunkIndex = 0
    private var blockWidthPx = 0

    internal fun buildBlockContent(
        text: String,
        textIndex: Int,
        blockIndex: Int,
        blockWidthPx: Int,
    ): WriterBlock {
        chunks.clear()
        lines.clear()
        this.text = text
        offsetX = 0
        lineTextIndex = 0
        lineChunkCount = 0
        startChunkIndex = 0
        this.blockWidthPx = blockWidthPx

        // Edge case: empty text → one empty line, zero chunks
        if (text.isEmpty() || blockWidthPx == 0) {
            return WriterBlock(
                text = text,
                chunks = chunks.toList(),
                lines = listOf(WriterLine.Empty),
                textIndex = textIndex,
                blockIndex = blockIndex,
            )
        }

        var blockTextIndex = 0
        var i = 0
        while (i < text.length) {
            val ch = text[i++]
            if (ch != ' ') continue
            parseChunk(blockTextIndex, i - 1)
            blockTextIndex = i
        }

        parseChunk(blockTextIndex, i)
        finishLine(text.length)

        return WriterBlock(
            text = text,
            chunks = chunks.toList(),
            lines = lines.toList(),
            textIndex = textIndex,
            blockIndex = blockIndex,
        )
    }

    private fun parseChunk(chunkStartIndex: Int, chunkEndIndex: Int) {
        val chunkText = text.substring(chunkStartIndex, chunkEndIndex)
        val layout = ruler.measure(chunkText, style)

        if (layout.size.width > blockWidthPx) {
            parseMultiChunk(chunkText, chunkStartIndex)
        } else {
            if (offsetX + layout.size.width > blockWidthPx) {
                finishLine(chunkStartIndex)
            }
            chunks.add(
                WriterChunk(
                    text = chunkText,
                    textLayout = layout,
                    blockTextIndex = chunkStartIndex,
                    isContinued = false,
                    offsetX = offsetX,
                    lineIndex = lines.size,
                    chunkIndex = chunks.size,
                )
            )
            lineChunkCount++
            offsetX += layout.size.width + spacePx
        }
    }

    fun parseMultiChunk(chunkText: String, startIndex: Int) {
        var subIndex = 0
        offsetX = 0
        while (subIndex < chunkText.length) {
            var endIndex = chunkText.length

            while (endIndex > subIndex) {
                val subChunk = chunkText.substring(subIndex, endIndex)
                val layout = ruler.measure(subChunk, style)
                if (layout.size.width > blockWidthPx) {
                    endIndex--
                } else {
                    val isContinued = (subIndex + subChunk.length) < chunkText.length
                    finishLine(startIndex + subIndex)
                    chunks.add(
                        WriterChunk(
                            text = subChunk,
                            textLayout = layout,
                            blockTextIndex = startIndex + subIndex,
                            isContinued = isContinued,
                            offsetX = 0,
                            lineIndex = lines.size,
                            chunkIndex = chunks.size,
                        )
                    )
                    lineChunkCount = 1
                    subIndex += subChunk.length
                    if (!isContinued) {
                        offsetX += layout.size.width + spacePx
                    }
                }
            }
        }
    }

    fun finishLine(newLineIndex: Int) {
        if (newLineIndex == lineTextIndex) return
        lines.add(
            WriterLine(
                blockTextIndex = lineTextIndex,
                length = newLineIndex - lineTextIndex,
                chunkIndex = startChunkIndex,
                chunkCount = lineChunkCount,
                lineIndex = lines.size
            )
        )
        lineTextIndex = newLineIndex
        lineChunkCount = 0
        startChunkIndex = chunks.size
        offsetX = 0
    }
}

private fun Char.isWordCharacter() = isLetterOrDigit() || this == '\''