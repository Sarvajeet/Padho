package com.padho.reader.data.translate

/**
 * Translates one semantic paragraph. Small paragraphs are sent as one request.
 * Large paragraphs are chunked only at sentence boundaries and processed in
 * order, with neighboring sentence context supplied to capable providers.
 *
 * Whitespace surrounding each translated chunk is kept from the source. This
 * prevents chunk reassembly from flattening line breaks or repeated spacing.
 */
class ParagraphTranslationCoordinator(
    private val provider: ContextualTranslationProvider,
    private val chunker: ContextualChunker = ContextualChunker()
) {
    suspend fun translate(request: ContextualTranslationRequest): String {
        if (request.paragraph.isBlank()) return request.paragraph

        val chunks = chunker.splitParagraph(request.paragraph)
        if (chunks.isEmpty()) return ""

        return buildString {
            for (index in chunks.indices) {
                val chunk = chunks[index]
                val source = chunk.text
                if (source.isBlank()) {
                    append(source)
                    continue
                }

                val leadingWhitespace = source.takeWhile { it.isWhitespace() }
                val trailingWhitespace = source.takeLastWhile { it.isWhitespace() }
                val bodyEnd = source.length - trailingWhitespace.length
                val body = source.substring(leadingWhitespace.length, bodyEnd)

                if (body.isEmpty()) {
                    append(source)
                    continue
                }

                val preceding = chunks.getOrNull(index - 1)?.text?.trim()
                    ?: request.precedingContext
                val following = chunks.getOrNull(index + 1)?.text?.trim()
                    ?: request.followingContext

                val translatedBody = provider.translate(
                    request.copy(
                        paragraph = body,
                        precedingContext = preceding,
                        followingContext = following
                    )
                ).trim()

                append(leadingWhitespace)
                append(translatedBody)
                append(trailingWhitespace)
            }
        }
    }
}
