package ponder.ember.ui

//internal fun measureLines(
//    chunks: List<WriterChunk>,
//    spacePx: Int,
//    blockWidthPx: Int,
//): List<WriterLine> {
//    val lines = mutableListOf<WriterLine>()
//    var currentWidth = 0
//    var startChunkIndex = 0
//    var wordCount = 0
//    chunks.forEachIndexed { index, chunk ->
//        val textLayout = chunk.textLayout
//        // println("chunk: $chunk currentWidth: $currentWidth")
//        val isLastWord = index == chunks.size - 1
//        val chunkWidth = textLayout.size.width + spacePx
//        if (chunkWidth + currentWidth > blockWidthPx || isLastWord) {
//            if (isLastWord) wordCount++
//            if (wordCount == 0) error("wordCount is zero")
//            lines.add(
//                WriterLine(
//                    chunkIndex = startChunkIndex,
//                    chunkCount = wordCount
//                )
//            )
//            startChunkIndex += wordCount
//            wordCount = 1
//            currentWidth = chunkWidth
//        } else {
//            currentWidth += chunkWidth
//            wordCount++
//        }
//    }
//    return lines
//}