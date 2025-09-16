package ponder.ember.app.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange

fun parseMarkdown(text: String): MarkdownBlock {
    return when {
        text.startsWith("> ") -> QuoteBlock(TextRange(0, 1), emptyList())
        text.startsWith("# ") -> HeadingBlock(1, TextRange(0, 1), emptyList())
        text.startsWith("## ") -> HeadingBlock(2, TextRange(0, 2), emptyList())
        text.startsWith("### ") -> HeadingBlock(3, TextRange(0, 3), emptyList())
        text.startsWith("#### ") -> HeadingBlock(4, TextRange(0, 4), emptyList())
        text.startsWith("##### ") -> HeadingBlock(5, TextRange(0, 5), emptyList())
        else -> ParagraphBlock(emptyList())
    }
}

sealed interface MarkdownBlock {
    val symbolRange: TextRange?
    val spans: List<MarkdownSpan>
}

data class ParagraphBlock(
    override val spans: List<MarkdownSpan>
): MarkdownBlock {
    override val symbolRange get() = null
}

data class QuoteBlock(
    override val symbolRange: TextRange,
    override val spans: List<MarkdownSpan>
): MarkdownBlock

data class HeadingBlock(
    val level: Int,
    override val symbolRange: TextRange,
    override val spans: List<MarkdownSpan>
): MarkdownBlock

sealed interface MarkdownSpan {
    val symbolRange: TextRange
    val contentRange: TextRange
}

data class ItalicSpan(
    override val symbolRange: TextRange,
    override val contentRange: TextRange,
): MarkdownSpan

data class BoldSpan(
    override val symbolRange: TextRange,
    override val contentRange: TextRange,
): MarkdownSpan

fun MarkdownBlock.getAnnotations(styleSet: StyleSet):  List<AnnotatedString.Range<AnnotatedString.Annotation>> {
    val symbolRange = symbolRange ?: return emptyList()
    return listOf(
        AnnotatedString.Range(styleSet.symbol, symbolRange.start, symbolRange.end)
    )
}