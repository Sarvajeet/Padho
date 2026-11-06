package com.padho.reader.data.translate

/**
 * A semantic translation request. The paragraph is the translation unit;
 * neighboring context and glossary are supporting information, not text to
 * duplicate in the rendered result.
 */
data class ContextualTranslationRequest(
    val paragraph: String,
    val targetLanguage: String,
    val sourceLanguage: String = "en",
    val precedingContext: String? = null,
    val followingContext: String? = null,
    val glossary: Map<String, String> = emptyMap(),
    val preserveParagraphFormatting: Boolean = true
) {
    init {
        require(paragraph.isNotBlank()) { "paragraph must not be blank" }
        require(sourceLanguage.isNotBlank()) { "sourceLanguage must not be blank" }
        require(targetLanguage.isNotBlank()) { "targetLanguage must not be blank" }
    }
}

/**
 * Provider boundary for both offline translation and context-aware cloud
 * translation. Implementations must translate the complete paragraph and
 * must not silently fall back to per-word translation.
 */
interface ContextualTranslationProvider {
    suspend fun translate(request: ContextualTranslationRequest): String
}
