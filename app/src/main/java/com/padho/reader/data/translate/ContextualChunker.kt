package com.padho.reader.data.translate

import java.text.BreakIterator
import java.util.Locale

/**
 * Splits one already-reconstructed semantic paragraph into translation requests.
 *
 * It deliberately never tokenizes on whitespace. Sentence boundaries are the
 * only normal split points, and an over-sized sentence is kept intact rather
 * than degraded into word-by-word translation.
 */
data class TranslationChunk(
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String
)

class ContextualChunker(
    private val maxChars: Int = 1_200,
    private val locale: Locale = Locale.ENGLISH
) {
    init {
        require(maxChars > 0) { "maxChars must be positive" }
    }

    fun splitParagraph(paragraph: String): List<TranslationChunk> {
        if (paragraph.isBlank()) return emptyList()

        val sentenceRanges = sentenceRanges(paragraph)
        if (sentenceRanges.isEmpty()) {
            return listOf(TranslationChunk(0, 0, paragraph.length, paragraph))
        }

        val chunks = mutableListOf<TranslationChunk>()
        var chunkStart = sentenceRanges.first().first
        var chunkEnd = sentenceRanges.first().last + 1

        for (rangeIndex in 1 until sentenceRanges.size) {
            val sentenceStart = sentenceRanges[rangeIndex].first
            val sentenceEnd = sentenceRanges[rangeIndex].last + 1

            // Keep original whitespace and punctuation between sentence ranges.
            if (sentenceEnd - chunkStart <= maxChars) {
                chunkEnd = sentenceEnd
            } else {
                chunks += TranslationChunk(
                    index = chunks.size,
                    startOffset = chunkStart,
                    endOffset = chunkEnd,
                    text = paragraph.substring(chunkStart, chunkEnd)
                )
                chunkStart = sentenceStart
                chunkEnd = sentenceEnd
            }
        }

        chunks += TranslationChunk(
            index = chunks.size,
            startOffset = chunkStart,
            endOffset = chunkEnd,
            text = paragraph.substring(chunkStart, chunkEnd)
        )

        return chunks
    }

    private fun sentenceRanges(text: String): List<IntRange> {
        val iterator = BreakIterator.getSentenceInstance(locale)
        iterator.setText(text)

        val ranges = mutableListOf<IntRange>()
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            if (end > start) ranges += start until end
            start = end
            end = iterator.next()
        }

        // ICU normally ends at text.length, but retain any trailing remainder.
        if (start < text.length) ranges += start until text.length
        return ranges
    }
}
