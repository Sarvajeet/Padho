package com.padho.reader.translation

/**
 * Replaceable translation boundary. Implementations may use on-device,
 * cloud-backed, or user-selected translation engines.
 */
interface TranslationProvider {
    /** Bump when output behavior changes so stale cached translations are bypassed. */
    val cacheVersion: String

    /**
     * Translate English source text into the requested ML Kit language code.
     * Implementations are suspending and should cooperate with coroutine cancellation.
     */
    suspend fun translate(sourceText: String, targetLanguageCode: String, onStatus: suspend (String) -> Unit = {}): String
}
