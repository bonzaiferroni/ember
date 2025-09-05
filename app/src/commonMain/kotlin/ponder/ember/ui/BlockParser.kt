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

        var segStart = 0
        var i = 0
        while (i <= text.length) {
            val atEnd = i == text.length
            val ch = if (!atEnd) text[i] else '\n'
            if (atEnd || ch == ' ') {
                if (segStart < i) {
                    parseChunk(segStart, i)
                }
                if (!atEnd) {
                    i++
                    segStart = i
                } else {
                    break
                }
            } else {
                i++
            }
        }

        finishLine(text.length)

        return WriterBlock(
            text = text,
            chunks = chunks.toList(),
            lines = lines.toList(),
            textIndex = textIndex,
            blockIndex = blockIndex,
        )
    }

    private fun parseChunk(chunkStart: Int, chunkEnd: Int) {
        if (chunkStart >= chunkEnd) return
        val chunkText = text.substring(chunkStart, chunkEnd)
        val layout = ruler.measure(chunkText, style)

        if (layout.size.width > blockWidthPx) {
            var subIndex = 0
            offsetX = 0
            while (subIndex < chunkText.length) {
                var endIndex = chunkText.length

                while (endIndex > subIndex) {
                    val subChunk = chunkText.substring(subIndex, endIndex)
                    val subLayout = ruler.measure(subChunk, style)
                    if (subLayout.size.width > blockWidthPx) {
                        endIndex--
                    } else {
                        val isContinued = (subIndex + subChunk.length) < chunkText.length
                        finishLine(chunkStart + subIndex)
                        chunks.add(
                            WriterChunk(
                                text = chunkText.substring(subIndex, subIndex + subChunk.length),
                                textLayout = subLayout,
                                blockTextIndex = chunkStart + subIndex,
                                isContinued = isContinued,
                                offsetX = 0,
                                lineIndex = lines.size,
                                chunkIndex = chunks.size,
                            )
                        )
                        lineChunkCount = 1
                        subIndex += subChunk.length
                        if (!isContinued) {
                            offsetX += subLayout.size.width + spacePx
                        }
                    }
                }
            }
        } else {
            if (offsetX + layout.size.width > blockWidthPx) {
                finishLine(chunkStart)
            }
            chunks.add(
                WriterChunk(
                    text = chunkText,
                    textLayout = layout,
                    blockTextIndex = chunkStart,
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