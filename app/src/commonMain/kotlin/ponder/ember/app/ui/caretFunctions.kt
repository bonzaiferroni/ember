package ponder.ember.app.ui

internal fun WriterState.setCaretAtIndex(
    bodyIndex: Int,
): Caret {
    val block = blocks.first { it.bodyRange.end >= bodyIndex }
    val paragraph = block.paragraph ?: error("paragraph not found")
    val contentIndex = bodyIndex - block.bodyRange.start
    val lineIndex = block.paragraph.getLineForOffset(contentIndex)
    val offsetX = paragraph.getHorizontalPosition(contentIndex, true)
    return Caret(
        bodyIndex = bodyIndex,
        contentIndex = contentIndex,
        blockIndex = block.blockIndex,
        lineIndex = lineIndex,
        offsetX = offsetX,
        preferredOffsetX = offsetX,
    )
}