package com.padho.reader.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageTextExtractorTest {

    @Test
    fun meaningfulTextRequiresAtLeastTwentyLettersOrDigits() {
        assertFalse(PdfPageTextExtractor.hasMeaningfulText(""))
        assertFalse(PdfPageTextExtractor.hasMeaningfulText("Only a few words"))
        assertTrue(PdfPageTextExtractor.hasMeaningfulText("This is a normal paragraph with enough readable content."))
    }

    @Test
    fun punctuationAndWhitespaceDoNotCountTowardThreshold() {
        assertFalse(PdfPageTextExtractor.hasMeaningfulText("---- ....   !!!"))
        assertTrue(PdfPageTextExtractor.hasMeaningfulText("12345678901234567890"))
    }
}
