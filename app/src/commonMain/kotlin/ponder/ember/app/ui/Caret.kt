package ponder.ember.app.ui

internal data class Caret(
    val bodyIndex: Int,
    val contentIndex: Int,
    val blockIndex: Int,
    val lineIndex: Int,
    val offsetX: Float,
    val preferredOffsetX: Float
) {
    companion object {
        val Home = Caret(0, 0, 0, 0, 0f, 0f)
    }
}

internal data class Selection(
    val start: Caret,
    val end: Caret
) {
    val isMultiBlock get() = start.blockIndex != end.blockIndex
    val length get() = end.bodyIndex - start.bodyIndex
}