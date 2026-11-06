package com.padho.reader.data.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

/**
 * Extracts visual text lines with PDFBox geometry, then groups those lines
 * into semantic paragraphs. This is a best-effort layout heuristic for
 * selectable-text PDFs; scanned pages still require OCR.
 */
class PdfParagraphExtractor(
    private val grouper: ParagraphGrouper = ParagraphGrouper()
) {
    fun extract(document: PDDocument, pageIndex: Int): List<String> {
        if (pageIndex !in 0 until document.numberOfPages) return emptyList()

        val pageWidth = document.getPage(pageIndex).mediaBox.width
        val stripper = PositionedTextStripper(pageWidth).apply {
            startPage = pageIndex + 1
            endPage = pageIndex + 1
            sortByPosition = true
        }
        stripper.getText(document)
        return grouper.group(stripper.positionedLines)
    }

    private class PositionedTextStripper(
        private val pageWidth: Float
    ) : PDFTextStripper() {
        val positionedLines = mutableListOf<PositionedLine>()
        private val lineText = StringBuilder()
        private val linePositions = mutableListOf<TextPosition>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            lineText.append(text)
            linePositions.addAll(textPositions)
        }

        override fun writeWordSeparator() {
            lineText.append(' ')
        }

        override fun writeLineSeparator() {
            flushLine()
        }

        override fun writeParagraphStart() {
            flushLine()
        }

        override fun writeParagraphEnd() {
            flushLine()
        }

        private fun flushLine() {
            val text = lineText.toString().trim()
            if (text.isNotEmpty()) {
                val valid = linePositions
                val xStart = valid.minOfOrNull { it.xDirAdj } ?: 0f
                val yTop = valid.minOfOrNull { it.yDirAdj - it.heightDir } ?: 0f
                val lineHeight = valid.maxOfOrNull { it.heightDir } ?: 10f
                val fontSize = if (valid.isEmpty()) 10f
                    else valid.map { it.fontSizeInPt }.average().toFloat()

                positionedLines += PositionedLine(
                    text = text,
                    xStart = xStart,
                    yTop = yTop,
                    lineHeight = lineHeight,
                    fontSize = fontSize,
                    pageWidth = pageWidth
                )
            }
            lineText.setLength(0)
            linePositions.clear()
        }
    }
}
