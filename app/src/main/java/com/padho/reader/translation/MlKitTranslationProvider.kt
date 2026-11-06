package com.padho.reader.translation

import com.google.android.gms.tasks.Tasks
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

    override fun translate(sourceText: String, targetLanguageCode: String): String {
        if (sourceText.isBlank()) return ""

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguageCode)
            .build()
        val translator = Translation.getClient(options)

        try {
            Tasks.await(translator.downloadModelIfNeeded())
            return TranslationTextChunker.paragraphs(sourceText).joinToString("\n\n") { chunks ->
                chunks.joinToString(" ") { chunk ->
                    Tasks.await(translator.translate(chunk))
                }
            }
        } finally {
            translator.close()
        }
    }
}
