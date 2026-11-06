package com.padho.reader.data.translate

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline ML Kit adapter. One translator/model session is reused across all
 * chunks of a page. ML Kit receives complete sentence-boundary chunks, never
 * individual tokens. Its on-device API cannot consume the adjacent context.
 */
class MlKitContextualTranslationProvider : ContextualTranslationProvider, AutoCloseable {
    private var translator: Translator? = null
    private var configuredSource: String? = null
    private var configuredTarget: String? = null
    private var modelReady = false

    override suspend fun translate(request: ContextualTranslationRequest): String =
        withContext(Dispatchers.IO) {
            val client = getOrCreateTranslator(request)
            if (!modelReady) {
                Tasks.await(client.downloadModelIfNeeded())
                modelReady = true
            }
            Tasks.await(client.translate(request.paragraph))
        }

    private fun getOrCreateTranslator(request: ContextualTranslationRequest): Translator {
        val current = translator
        if (current != null &&
            configuredSource == request.sourceLanguage &&
            configuredTarget == request.targetLanguage
        ) return current

        current?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(request.sourceLanguage)
            .setTargetLanguage(request.targetLanguage)
            .build()
        return Translation.getClient(options).also {
            translator = it
            configuredSource = request.sourceLanguage
            configuredTarget = request.targetLanguage
            modelReady = false
        }
    }

    override fun close() {
        translator?.close()
        translator = null
        configuredSource = null
        configuredTarget = null
        modelReady = false
    }
}
