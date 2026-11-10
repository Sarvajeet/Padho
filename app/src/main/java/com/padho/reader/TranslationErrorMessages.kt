package com.padho.reader

internal object TranslationErrorMessages {
    fun friendly(error: Exception): String {
        val detail = error.message.orEmpty().lowercase()
        return when {
            listOf("network", "unable to resolve host", "unknownhost", "connection", "timeout").any { it in detail } ->
                "Couldn't download the language model. Check your internet connection and try again."
            listOf("model", "download").any { it in detail } ->
                "The language model couldn't be prepared. Connect to the internet and retry."
            listOf("unsupported", "not supported").any { it in detail } ->
                "This translation language isn't supported by the current engine."
            else -> "Translation couldn't be completed. Please try again."
        }
    }
}
