package com.padho.reader.data.pdf

/**
 * Geometry captured from one visually rendered PDF text line.
 *
 * Coordinates are normalized to a top-down page coordinate system before
 * reaching this class. The grouping thresholds are conservative heuristics,
 * not a claim to reproduce the original APK's private thresholds.
 */
data class PositionedLine(
    val text: String,
    val xStart: Float,
    val yTop: Float,
    val lineHeight: Float,
    val fontSize: Float,
    val pageWidth: Float
)

class ParagraphGrouper(
    private val paragraphGapFactor: Float = 0.75f,
    private val maxIndentDeltaInFontSizes: Float = 2.0f,
    private val maxFontSizeRatio: Float = 1.2f
) {
    init {
        require(paragraphGapFactor > 0f)
        require(maxIndentDeltaInFontSizes > 0f)
        require(maxFontSizeRatio >= 1f)
    }

    fun group(lines: List<PositionedLine>): List<String> {
        val usable = lines.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return emptyList()

        val paragraphs = mutableListOf<StringBuilder>()
        var previous: PositionedLine? = null

        for (line in usable) {
            val prior = previous
            val startsNewParagraph = prior == null || separates(prior, line)
            if (startsNewParagraph) {
                paragraphs += StringBuilder(line.text.trim())
            } else {
                paragraphs.last().append(' ').append(line.text.trim())
            }
            previous = line
        }

        return paragraphs.map { it.toString() }
    }

    private fun separates(previous: PositionedLine, current: PositionedLine): Boolean {
        val priorHeight = previous.lineHeight.coerceAtLeast(1f)
        val currentHeight = current.lineHeight.coerceAtLeast(1f)
        val verticalGap = current.yTop - (previous.yTop + priorHeight)
        if (verticalGap > priorHeight * paragraphGapFactor) return true

        val priorFont = previous.fontSize.coerceAtLeast(1f)
        val currentFont = current.fontSize.coerceAtLeast(1f)
        val fontRatio = maxOf(priorFont, currentFont) / minOf(priorFont, currentFont)
        if (fontRatio > maxFontSizeRatio) return true

        val indentDelta = kotlin.math.abs(current.xStart - previous.xStart)
        val indentLimit = maxOf(priorFont, currentFont) * maxIndentDeltaInFontSizes
        if (indentDelta > indentLimit) return true

        if (startsListItem(current.text)) return true

        // A large change in page width usually indicates a different column
        // or text region. Do not stitch those lines into one paragraph.
        val widthBase = previous.pageWidth.coerceAtLeast(1f)
        if (kotlin.math.abs(current.pageWidth - previous.pageWidth) / widthBase > 0.08f) return true

        // A small negative gap can occur with overlapping glyph boxes; keep it.
        return false
    }

    private fun startsListItem(text: String): Boolean =
        LIST_ITEM.containsMatchIn(text.trimStart())

    private companion object {
        // Raw Kotlin strings need a single backslash for regex whitespace.
        val LIST_ITEM = Regex("""^(?:[•◦▪‣*-]|\\(?\\d+[.)]|[A-Za-z][.)])\\s+""")
    }
}
