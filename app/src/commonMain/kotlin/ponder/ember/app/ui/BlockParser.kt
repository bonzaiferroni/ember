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
        text: String,
        textIndex: Int,
        blockIndex: Int,
        blockWidthPx: Int,
    ): WriterBlock {

        val paragraph = Paragraph(
            paragraphIntrinsics = ParagraphIntrinsics(
                text = text,
                style = style,
                annotations = emptyList(),
                density = density,
                fontFamilyResolver = resolver,
            ),
            constraints = Constraints(maxWidth = blockWidthPx)
        )

        return WriterBlock(text, paragraph, textIndex, blockIndex)
    }
}