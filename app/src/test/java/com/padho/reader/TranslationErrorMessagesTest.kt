package com.padho.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationErrorMessagesTest {

    @Test
    fun explainsNetworkFailures() {
        assertEquals(
            "Couldn't download the language model. Check your internet connection and try again.",
            TranslationErrorMessages.friendly(Exception("Unable to resolve host"))
        )
    }

    @Test
    fun explainsModelDownloadFailures() {
        assertEquals(
            "The language model couldn't be prepared. Connect to the internet and retry.",
            TranslationErrorMessages.friendly(Exception("Model download failed"))
        )
    }

    @Test
    fun explainsUnsupportedLanguages() {
        assertEquals(
            "This translation language isn't supported by the current engine.",
            TranslationErrorMessages.friendly(Exception("Language not supported"))
        )
    }

    @Test
    fun providesSafeFallbackForUnknownFailures() {
        assertEquals(
            "Translation couldn't be completed. Please try again.",
            TranslationErrorMessages.friendly(Exception("Unexpected internal error"))
        )
    }
}
