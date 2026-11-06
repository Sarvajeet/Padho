package com.padho.reader.data.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ContextualChunkerTest {
    @Test
    fun keepsShortParagraphAsOneSemanticUnit() {
        val paragraph = "The road was long. But she kept walking."
        val chunks = ContextualChunker(maxChars = 500, locale = Locale.ENGLISH)
            .splitParagraph(paragraph)

        assertEquals(1, chunks.size)
        assertEquals(paragraph, chunks.single().text)
    }

    @Test
    fun splitsLongParagraphOnlyAtSentenceBoundaries() {
        val paragraph = "First complete sentence. Second complete sentence. Third complete sentence."
        val chunks = ContextualChunker(maxChars = 35, locale = Locale.ENGLISH)
            .splitParagraph(paragraph)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.text.endsWith(".") || it.text.endsWith(". ") })
        assertEquals(paragraph, chunks.joinToString("") { it.text })
    }

    @Test
    fun neverSplitsAnOversizedSentenceIntoWords() {
        val sentence = "A".repeat(160) + " remains one sentence."
        val chunks = ContextualChunker(maxChars = 40, locale = Locale.ENGLISH)
            .splitParagraph(sentence)

        assertEquals(1, chunks.size)
        assertEquals(sentence, chunks.single().text)
    }

    @Test
    fun preservesExactSourceOffsetsAndWhitespace() {
        val paragraph = "First sentence.  Second sentence!\nThird sentence?"
        val chunks = ContextualChunker(maxChars = 20, locale = Locale.ENGLISH)
            .splitParagraph(paragraph)

        assertEquals(paragraph, chunks.joinToString("") { it.text })
        chunks.forEach { chunk ->
            assertEquals(chunk.text, paragraph.substring(chunk.startOffset, chunk.endOffset))
        }
    }

    @Test
    fun blankInputProducesNoRequests() {
        assertTrue(ContextualChunker().splitParagraph(" \n ").isEmpty())
    }
}
