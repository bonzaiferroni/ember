package ponder.ember.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

class BlockParser(
    private val styles: StyleSet,
    private val density: Density,
    private val resolver: FontFamily.Resolver,
) {
    internal fun buildBlockContent(
        content: String,
        bodyIndex: Int,
        blockIndex: Int,
        blockWidthPx: Int,
    ): TextBlock {

        val markdown = parseMarkdown(content)
        val annotations = markdown.getAnnotations(styles)

        val paragraph = Paragraph(
            paragraphIntrinsics = ParagraphIntrinsics(
                text = content,
                style = styles.getTextStyle(markdown),
                annotations = annotations,
                density = density,
                fontFamilyResolver = resolver,
            ),
            constraints = Constraints(maxWidth = blockWidthPx)
        )

        return TextBlock(
            content = content,
            paragraph = paragraph,
            lines = List(paragraph.lineCount) { lineIndex ->
                val contentIndex = paragraph.getLineStart(lineIndex)
                TextLine(
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
            markdown = markdown,
        )
    }
}

data class StyleSet(
    val paragraph: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val symbolColor: Color
) {
    val blockQuote = paragraph.copy(fontStyle = FontStyle.Italic)
    val symbol = paragraph.toSpanStyle().copy(color = symbolColor)
    val italic = paragraph.toSpanStyle().copy(fontStyle = FontStyle.Italic)
    val bold = paragraph.toSpanStyle().copy(fontWeight = FontWeight.Bold)
    val italicAndBold = paragraph.toSpanStyle().copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)

    fun getTextStyle(block: MarkdownBlock) = when(block) {
        is HeadingBlock -> when (block.level) {
            1 -> h1
            2 -> h2
            3 -> h3
            4 -> h4
            5 -> h5
            else -> error("invalid heading level: ${block.level}")
        }
        is ParagraphBlock -> paragraph
        is QuoteBlock -> blockQuote
    }
}