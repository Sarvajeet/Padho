package com.padho.reader.data.translate

/**
 * Translates one semantic paragraph. Small paragraphs are sent as one request.
 * Large paragraphs are chunked only at sentence boundaries and processed in
 * order, with neighboring sentence context supplied to capable providers.
 */
class ParagraphTranslationCoordinator(
    private val provider: ContextualTranslationProvider,
    private val chunker: ContextualChunker = ContextualChunker()
) {
    suspend fun translate(request: ContextualTranslationRequest): String {
        val chunks = chunker.splitParagraph(request.paragraph)
        if (chunks.isEmpty()) return ""

        val translated = ArrayList<String>(chunks.size)
        for (index in chunks.indices) {
            val chunk = chunks[index]
            val preceding = chunks.getOrNull(index - 1)?.text ?: request.precedingContext
            val following = chunks.getOrNull(index + 1)?.text ?: request.followingContext

            translated += provider.translate(
                request.copy(
                    paragraph = chunk.text.trim(),
                    precedingContext = preceding,
                    followingContext = following
                )
            ).trim()
        }

        return translated.filter { it.isNotEmpty() }.joinToString(" ")
    }
}
