package com.padho.reader.data.translate

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ParagraphTranslationCoordinatorTest {
    @Test
    fun sendsShortParagraphAsOneRequest() = runBlocking {
        val received = mutableListOf<ContextualTranslationRequest>()
        val provider = object : ContextualTranslationProvider {
            override suspend fun translate(request: ContextualTranslationRequest): String {
                received += request
                return request.paragraph
            }
        }
        val paragraph = "The river was silent. The village was asleep."

        val result = ParagraphTranslationCoordinator(
            provider,
            ContextualChunker(maxChars = 500, locale = Locale.ENGLISH)
        ).translate(ContextualTranslationRequest(paragraph, targetLanguage = "hi"))

        assertEquals(paragraph, result)
        assertEquals(1, received.size)
        assertEquals(paragraph, received.single().paragraph)
    }

    @Test
    fun suppliesAdjacentChunkContextForLongParagraphs() = runBlocking {
        val received = mutableListOf<ContextualTranslationRequest>()
        val provider = object : ContextualTranslationProvider {
            override suspend fun translate(request: ContextualTranslationRequest): String {
                received += request
                return request.paragraph
            }
        }
        val paragraph = "First sentence is complete. Second sentence is complete. Third sentence is complete."

        ParagraphTranslationCoordinator(
            provider,
            ContextualChunker(maxChars = 35, locale = Locale.ENGLISH)
        ).translate(ContextualTranslationRequest(paragraph, targetLanguage = "gu"))

        assertTrue(received.size > 1)
        assertEquals(null, received.first().precedingContext)
        assertTrue(received.first().followingContext!!.isNotBlank())
        assertTrue(received.last().precedingContext!!.isNotBlank())
        assertEquals(null, received.last().followingContext)
    }
}
