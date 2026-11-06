package com.padho.reader.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Extracts selectable text from one PDF page.
 *
 * Kept outside the Compose UI so extraction can evolve independently
 * (for example, OCR fallback and document-session reuse).
 */
object PdfTextExtractor {

    fun extractPageText(context: Context, uri: Uri, pageIndex: Int): String {
        require(pageIndex >= 0) { "Page index must not be negative." }

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open this PDF.")

        input.use { stream ->
            PDDocument.load(stream).use { document ->
                if (pageIndex >= document.numberOfPages) return ""
                val stripper = PDFTextStripper().apply {
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                    sortByPosition = true
                }

                return normalize(stripper.getText(document))
            }
        }
    }

    private fun normalize(rawText: String): String =
        rawText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split(Regex("\\n\\s*\\n+"))
            .map { paragraph ->
                paragraph.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
            }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
}
