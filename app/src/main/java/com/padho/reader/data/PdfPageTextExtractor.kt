package com.padho.reader.data

import android.content.Context
import android.net.Uri

/**
 * Prefers embedded/selectable PDF text and falls back to on-device OCR
 * only when the extracted page contains too little readable text.
 */
object PdfPageTextExtractor {

    fun extractPageText(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        session: PdfDocumentSession
    ): String {
        val selectableText = PdfTextExtractor.extractPageText(context, uri, pageIndex)
        if (hasMeaningfulText(selectableText)) return selectableText

        val bitmap = session.renderPage(uri, pageIndex, OCR_RENDER_WIDTH) ?: return selectableText
        val recognizedText = try {
            OcrTextExtractor.extractText(bitmap)
        } finally {
            bitmap.recycle()
        }

        return if (hasMeaningfulText(recognizedText)) recognizedText else selectableText
    }

    internal fun hasMeaningfulText(text: String): Boolean =
        text.count { it.isLetterOrDigit() } >= MIN_SELECTABLE_CHARACTERS

    private const val MIN_SELECTABLE_CHARACTERS = 20
    private const val OCR_RENDER_WIDTH = 1600
}
