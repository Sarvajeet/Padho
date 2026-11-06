package com.padho.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationTextChunkerTest {

    @Test
    fun preservesParagraphBoundaries() {
        val chunks = TranslationTextChunker.paragraphs(
            "First paragraph. It has two sentences.\n\nSecond paragraph."
        )

        assertEquals(2, chunks.size)
        assertEquals("First paragraph. It has two sentences.", chunks[0].single())
        assertEquals("Second paragraph.", chunks[1].single())
    }

    @Test
    fun groupsSentencesWithoutExceedingLimitWherePossible() {
        val chunks = TranslationTextChunker.paragraphs(
            "One short sentence. Two short sentence. Three short sentence.",
            maxChars = 35
        ).flatten()

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 35 })
        assertEquals(
            "One short sentence. Two short sentence. Three short sentence.",
            chunks.joinToString(" ")
        )
    }

    @Test
    fun splitsOversizedSentenceAtWordBoundaries() {
        val source = "word ".repeat(30).trim()
        val chunks = TranslationTextChunker.paragraphs(source, maxChars = 20).flatten()

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 20 })
        assertEquals(source, chunks.joinToString(" "))
    }

    @Test
    fun ignoresBlankParagraphs() {
        assertTrue(TranslationTextChunker.paragraphs(" \n\n  ").isEmpty())
    }
}
