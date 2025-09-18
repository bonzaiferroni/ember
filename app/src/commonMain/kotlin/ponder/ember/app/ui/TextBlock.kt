package ponder.ember.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import kabinet.utils.randomUuidString

@Stable
internal data class TextBlock(
    val textBlockId: TextBlockId,
    val content: String,
    override val markdown: MarkdownBlock,
    val paragraph: Paragraph?,
    val lines: List<TextLine>,
    val blockIndex: Int,
    val bodyIndex: Int,
): ParsedBlock {
    val bodyIndexEnd get() = bodyIndex + content.length

    companion object {
        val Empty = TextBlock(
            textBlockId = TextBlockId.random(),
            content = "",
            markdown = ParagraphBlock(emptyList()),
            paragraph = null,
            lines = emptyList(),
            blockIndex = 0,
            bodyIndex = 0,
        )
    }
}

@JvmInline
value class TextBlockId(val value: String) {
    companion object { fun random() = TextBlockId(randomUuidString()) }
}

internal data class TextLine(
    val lineIndex: Int,
    val bodyIndex: Int,
    val contentIndex: Int,
    val length: Int,
    val width: Float,
    val height: Float,
    val left: Float,
    val top: Float,
    val isLast: Boolean,
    val isFirst: Boolean,
) {
    val contentIndexEnd get() = contentIndex + length
    val bodyIndexEnd get() = bodyIndex + length
    val right get() = left + width
}