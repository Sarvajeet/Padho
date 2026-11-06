package com.padho.reader.translation

import kotlinx.coroutines.tasks.await
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.padho.reader.data.TranslationTextChunker

/**
 * Baseline on-device provider backed by Google ML Kit.
 * Models are downloaded on first use and then translation runs on-device.
 */
class MlKitTranslationProvider : TranslationProvider {
    override val cacheVersion: String = "mlkit-v3"

    override suspend fun translate(sourceText: String, targetLanguageCode: String): String {
        if (sourceText.isBlank()) return ""

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguageCode)
            .build()
        val translator = Translation.getClient(options)

        try {
            translator.downloadModelIfNeeded().await()
            val translatedParagraphs = mutableListOf<String>()
            for (paragraphChunks in TranslationTextChunker.paragraphs(sourceText)) {
                val translatedChunks = mutableListOf<String>()
                for (chunk in paragraphChunks) {
                    translatedChunks += translator.translate(chunk).await()
                }
                translatedParagraphs += translatedChunks.joinToString(" ")
            }
            return translatedParagraphs.joinToString("\n\n")
        } finally {
            translator.close()
        }
    }
}
