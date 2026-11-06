package com.padho.reader.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrFallbackPolicyTest {

    @Test
    fun usesEmbeddedTextWhenItHasEnoughReadableCharacters() {
        assertTrue(PdfPageTextExtractor.hasMeaningfulText("This is a selectable PDF page with enough text."))
    }

    @Test
    fun sendsImageOnlyPagesToOcr() {
        assertFalse(PdfPageTextExtractor.hasMeaningfulText(""))
        assertFalse(PdfPageTextExtractor.hasMeaningfulText("   "))
        assertFalse(PdfPageTextExtractor.hasMeaningfulText("Page 7"))
    }

    @Test
    fun treatsSparseTextAsAnOcrCandidate() {
        assertFalse(PdfPageTextExtractor.hasMeaningfulText("A short heading"))
    }

    @Test
    fun acceptsTextAtTheFallbackThreshold() {
        assertTrue(PdfPageTextExtractor.hasMeaningfulText("12345678901234567890"))
    }
}
