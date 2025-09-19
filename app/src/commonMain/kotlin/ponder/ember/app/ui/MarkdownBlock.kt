package ponder.ember.app.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange

sealed interface MarkdownBlock {
    val markRange: TextRange?
    val spans: List<MarkdownSpan>
}

data class ParagraphBlock(
    override val spans: List<MarkdownSpan>
): MarkdownBlock {
    override val markRange get() = null
}

data class QuoteBlock(
    override val markRange: TextRange,
    override val spans: List<MarkdownSpan>
): MarkdownBlock

data class HeadingBlock(
    val level: Int,
    override val markRange: TextRange,
    override val spans: List<MarkdownSpan>
): MarkdownBlock

sealed interface MarkdownSpan {
    val markRange: TextRange
    val contentRange: TextRange
}

data class ItalicSpan(
    override val markRange: TextRange,
    override val contentRange: TextRange,
): MarkdownSpan

data class BoldSpan(
    override val markRange: TextRange,
    override val contentRange: TextRange,
): MarkdownSpan

data class BoldItalicSpan(
    override val markRange: TextRange,
    override val contentRange: TextRange,
): MarkdownSpan

fun MarkdownBlock.getAnnotations(styleSet: StyleSet): List<AnnotatedString.Range<AnnotatedString.Annotation>> {
    val annotations = mutableListOf<AnnotatedString.Range<AnnotatedString.Annotation>>()
    markRange?.let {
        annotations.add(AnnotatedString.Range(styleSet.symbol, it.start, it.end))
    }
    spans.forEach { span ->
        annotations.add(AnnotatedString.Range(styleSet.symbol, span.markRange.start, span.markRange.end))
        annotations.add(AnnotatedString.Range(span.toSpanStyle(styleSet), span.contentRange.start, span.contentRange.end))
    }
    return annotations
}

fun MarkdownSpan.toSpanStyle(styleSet: StyleSet) = when (this) {
    is BoldItalicSpan -> styleSet.italicAndBold
    is BoldSpan -> styleSet.bold
    is ItalicSpan -> styleSet.italic
}

fun blockLevelOf(text: String) = when {
    text.startsWith("# ") -> 1
    text.startsWith("## ") -> 2
    text.startsWith("### ") -> 3
    text.startsWith("#### ") -> 4
    text.startsWith("##### ") -> 5
    text.length > 10 -> 0
    else -> null
}