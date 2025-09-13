package ponder.ember.app.ui

import androidx.compose.ui.text.TextLayoutResult
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
    val chunks = mutableListOf<WriterChunk>()
    val lines = mutableListOf<WriterLine>()
    var index = 0
    var offsetX = 0
    var lineTextIndex = 0
    var lineChunkCount = 0
    var startChunkIndex = 0

    fun finishLine(newLineIndex: Int) {
        if (newLineIndex == lineTextIndex) return
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

internal fun parseBlockContent2(
    text: String,
    textIndex: Int,
    blockIndex: Int,
    ruler: TextMeasurer,
    style: TextStyle,
    blockWidthPx: Int,
    spacePx: Int,
): WriterBlock {
    val chunks = mutableListOf<WriterChunk>()
    val lines = mutableListOf<WriterLine>()

    // Edge case: empty text → one empty line, zero chunks
    if (text.isEmpty()) {
        lines.add(
            WriterLine(
                blockTextIndex = 0,
                length = 0,
                chunkIndex = 0,
                chunkCount = 0,
                lineIndex = 0
            )
        )
        return WriterBlock(
            text = text,
            chunks = chunks,
            lines = lines,
            textIndex = textIndex,
            blockIndex = blockIndex,
        )
    }

    var offsetX = 0
    var lineTextIndex = 0
    var lineChunkCount = 0
    var startChunkIndex = 0

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
    }

    fun placeSegment(chunkStart: Int, chunkEnd: Int) {
        if (chunkStart >= chunkEnd) return
        val chunk = text.substring(chunkStart, chunkEnd)
        val layout = ruler.measure(chunk, style)

        if (layout.size.width > blockWidthPx) {
            var subIndex = 0
            offsetX = 0
            while (subIndex < chunk.length) {
                var endIndex = chunk.length
                var fittedLayout: TextLayoutResult? = null
                var fittedLen = 0

                while (endIndex > subIndex) {
                    val candidate = chunk.substring(subIndex, endIndex)
                    val candLayout = ruler.measure(candidate, style)
                    if (candLayout.size.width > blockWidthPx) {
                        endIndex--
                    } else {
                        fittedLayout = candLayout
                        fittedLen = candidate.length
                        break
                    }
                }

                if (fittedLen == 0) {
                    val fallback = chunk[subIndex].toString()
                    val fbLayout = ruler.measure(fallback, style)
                    finishLine(chunkStart + subIndex)
                    chunks.add(
                        WriterChunk(
                            text = fallback,
                            textLayout = fbLayout,
                            blockTextIndex = chunkStart + subIndex,
                            isContinued = (subIndex + 1) < chunk.length,
                            offsetX = 0,
                            lineIndex = lines.size,
                            chunkIndex = chunks.size,
                        )
                    )
                    lineChunkCount = 1
                    subIndex += 1
                    if (subIndex >= chunk.length) {
                        offsetX += fbLayout.size.width + spacePx
                    }
                } else {
                    val isContinued = (subIndex + fittedLen) < chunk.length
                    finishLine(chunkStart + subIndex)
                    chunks.add(
                        WriterChunk(
                            text = chunk.substring(subIndex, subIndex + fittedLen),
                            textLayout = fittedLayout!!,
                            blockTextIndex = chunkStart + subIndex,
                            isContinued = isContinued,
                            offsetX = 0,
                            lineIndex = lines.size,
                            chunkIndex = chunks.size,
                        )
                    )
                    lineChunkCount = 1
                    subIndex += fittedLen
                    if (!isContinued) {
                        offsetX += fittedLayout.size.width + spacePx
                    }
                }
            }
        } else {
            if (offsetX + layout.size.width > blockWidthPx) {
                finishLine(chunkStart)
                offsetX = 0
            }
            chunks.add(
                WriterChunk(
                    text = chunk,
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

    var segStart = 0
    var i = 0
    val n = text.length
    while (i <= n) {
        val atEnd = i == n
        val ch = if (!atEnd) text[i] else '\n'
        if (atEnd || ch == ' ') {
            if (segStart < i) {
                placeSegment(segStart, i)
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

    finishLine(n)

    return WriterBlock(
        text = text,
        chunks = chunks,
        lines = lines,
        textIndex = textIndex,
        blockIndex = blockIndex,
    )
}

