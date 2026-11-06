package com.padho.reader.data.translate

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline ML Kit adapter. ML Kit receives a complete paragraph-sized chunk,
 * never individual tokens. The adjacent context remains available to richer
 * providers; the on-device ML Kit API has no context-prompt parameter.
 */
class MlKitContextualTranslationProvider : ContextualTranslationProvider {
    override suspend fun translate(request: ContextualTranslationRequest): String =
        withContext(Dispatchers.IO) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(request.sourceLanguage)
                .setTargetLanguage(request.targetLanguage)
                .build()
            val translator = Translation.getClient(options)
            try {
                Tasks.await(translator.downloadModelIfNeeded())
                Tasks.await(translator.translate(request.paragraph))
            } finally {
                translator.close()
            }
        }
}
