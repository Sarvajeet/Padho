package com.padho.reader.data

import java.text.BreakIterator
import java.util.Locale

/**
 * Groups complete English sentences into context-rich translation requests.
 * Paragraph boundaries are retained; long paragraphs are split only between
 * sentences where possible, with a word-boundary fallback for oversized ones.
 */
object TranslationTextChunker {
    private const val DEFAULT_MAX_CHARS = 900

    fun paragraphs(text: String, maxChars: Int = DEFAULT_MAX_CHARS): List<List<String>> {
        require(maxChars > 0) { "Maximum chunk length must be positive." }

        return text.split(Regex("\\n\\s*\\n+"))
            .mapNotNull { paragraph ->
                val normalized = paragraph.replace(Regex("\\s+"), " ").trim()
                if (normalized.isBlank()) null else splitParagraph(normalized, maxChars)
            }
    }

    private fun splitParagraph(paragraph: String, maxChars: Int): List<String> {
        val iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH)
        iterator.setText(paragraph)
        val sentences = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val sentence = paragraph.substring(start, end).trim()
            if (sentence.isNotEmpty()) sentences += sentence
            start = end
            end = iterator.next()
        }
        if (sentences.isEmpty()) sentences += paragraph

        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        for (sentence in sentences) {
            val pieces = if (sentence.length > maxChars) splitAtWords(sentence, maxChars) else listOf(sentence)
            for (piece in pieces) {
                val candidateLength = current.length + if (current.isEmpty()) 0 else 1 + piece.length
                if (candidateLength > maxChars && current.isNotEmpty()) {
                    chunks += current.toString()
                    current = StringBuilder()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(piece)
            }
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks
    }

    private fun splitAtWords(text: String, maxChars: Int): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(Regex("\\s+"))) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                result += current.toString()
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }
}
