package ponder.ember.app.ui

import androidx.compose.ui.text.TextRange

class MarkdownParser {
    private val spans = mutableListOf<MarkdownSpan>()
    private val stack = ArrayDeque<Delimiter>()
    private var text = ""

    fun parse(text: String): MarkdownBlock {
        this.text = text
        val spans = parseSpans()

        return when {
            text.startsWith("> ") -> QuoteBlock(TextRange(0, 1), spans)
            text.startsWith("# ") -> HeadingBlock(1, TextRange(0, 1), spans)
            text.startsWith("## ") -> HeadingBlock(2, TextRange(0, 2), spans)
            text.startsWith("### ") -> HeadingBlock(3, TextRange(0, 3), spans)
            text.startsWith("#### ") -> HeadingBlock(4, TextRange(0, 4), spans)
            text.startsWith("##### ") -> HeadingBlock(5, TextRange(0, 5), spans)
            else -> ParagraphBlock(spans)
        }
    }

    fun parseSpans(): List<MarkdownSpan> {
        spans.clear()
        stack.clear()
        var i = 0
        val n = text.length

        while (i < n) {
            val c = text[i]

            // Escape sequence
            if (c == '\\' && (text.isCharAt(i + 1, '*') || text.isCharAt(i + 1, '_') || text.isCharAt(i + 1, '\\'))) {
                i += 2
                continue
            }

            if (c == '*' || c == '_') {
                var j = i
                while (j < n && text[j] == c && j - i < 3) j++
                val runLen = j - i
                val openable = text.canOpenRun(i, j, c)
                val closable = text.canCloseRun(i, j, c)

                var remainingClose = if (closable) runLen else 0
                var consumedFromCloserStart = 0

                while (remainingClose > 0) {
                    var k = stack.lastIndex
                    while (k >= 0 && stack[k].char != c) k--
                    if (k < 0) break

                    val opener = stack.removeAt(k)
                    val take = bestTake(opener.count, remainingClose)

                    val markStart = opener.start
                    val markEnd = i + consumedFromCloserStart + take
                    val contentStart = markStart + take
                    val contentEnd = markEnd - take

                    when (take) {
                        3 -> spans += BoldItalicSpan(TextRange(markStart, markEnd), TextRange(contentStart, contentEnd))
                        2 -> spans += BoldSpan(TextRange(markStart, markEnd), TextRange(contentStart, contentEnd))
                        else -> spans += ItalicSpan(TextRange(markStart, markEnd), TextRange(contentStart, contentEnd))
                    }

                    if (opener.count > take) {
                        stack.add(Delimiter(c, opener.count - take, opener.start + take))
                    }

                    remainingClose -= take
                    consumedFromCloserStart += take
                }

                if (remainingClose > 0 && openable) {
                    stack.add(Delimiter(c, remainingClose, i + consumedFromCloserStart))
                } else if (!closable && openable) {
                    stack.add(Delimiter(c, runLen, i))
                }

                i = j
                continue
            }

            i++
        }

        return spans.sortedBy { it.markRange.start }
    }
}

private fun String.countCharAt(index: Int, char: Char): Int {
    var count = 0
    if (isCharAt(index, char)) count++
    else return count
    if (isCharAt(index + 1, char)) count++
    else return count
    if (isCharAt(index + 2, char)) count++
    return count
}

private fun String.isCharAt(index: Int, char: Char): Boolean {
    return if (length <= index) false
    else this[index] == char
}

private fun markRange(startIndex: Int, endIndex: Int, markLength: Int) =
    TextRange(startIndex - markLength, endIndex + markLength)

private data class Delimiter(
    val char: Char,              // '*' or '_'
    var count: Int,            // 1..3
    val start: Int,            // index of first marker
)

fun Char?.isWord() = this != null && this.isLetterOrDigit()
fun Char?.isSpace() = this == null || this.isWhitespace()
fun Char?.isPunctuation() = this != null && !this.isLetterOrDigit() && !this.isWhitespace()

fun String.canOpenRun(start: Int, endExclusive: Int, ch: Char): Boolean {
    val prev = getOrNull(start - 1)
    val next = getOrNull(endExclusive)
    if (ch == '_' && prev.isWord() && next.isWord()) return false
    val leftFlanking = !next.isSpace() &&
            !(next.isPunctuation() && !prev.isSpace() && !prev.isPunctuation())
    return leftFlanking
}

fun String.canCloseRun(start: Int, endExclusive: Int, ch: Char): Boolean {
    val prev = getOrNull(start - 1)
    val next = getOrNull(endExclusive)
    if (ch == '_' && prev.isWord() && next.isWord()) return false
    val rightFlanking = !prev.isSpace() &&
            !(prev.isPunctuation() && !next.isSpace() && !next.isPunctuation())
    return rightFlanking
}

fun bestTake(a: Int, b: Int): Int =
    if (a >= 3 && b >= 3) 3 else if (a >= 2 && b >= 2) 2 else 1