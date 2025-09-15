package ponder.ember.app.ui

import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

class BlockParser(
    private val style: TextStyle,
    private val density: Density,
    private val resolver: FontFamily.Resolver
) {

    internal fun buildBlockContent(
        content: String,
        bodyIndex: Int,
        blockIndex: Int,
        blockWidthPx: Int,
    ): WriterBlock {

        val paragraph = Paragraph(
            paragraphIntrinsics = ParagraphIntrinsics(
                text = content,
                style = style,
                annotations = emptyList(),
                density = density,
                fontFamilyResolver = resolver,
            ),
            constraints = Constraints(maxWidth = blockWidthPx)
        )

        return WriterBlock(
            content = content,
            paragraph = paragraph,
            lines = List(paragraph.lineCount) { lineIndex ->
                val contentIndex = paragraph.getLineStart(lineIndex)
                WriterLine(
                    lineIndex = lineIndex,
                    bodyIndex = bodyIndex + contentIndex,
                    contentIndex = contentIndex,
                    length = paragraph.getLineEnd(lineIndex) - contentIndex,
                    width = paragraph.getLineWidth(lineIndex),
                    height = paragraph.getLineHeight(lineIndex),
                    left = paragraph.getLineLeft(lineIndex),
                    top = paragraph.getLineTop(lineIndex),
                    isLast = lineIndex == paragraph.lineCount - 1,
                    isFirst = lineIndex == 0
                )
            },
            blockIndex = blockIndex,
            bodyIndex = bodyIndex,
        )
    }
}