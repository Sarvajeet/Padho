package com.padho.reader.data.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class ParagraphGrouperTest {
    private val grouper = ParagraphGrouper()

    @Test
    fun joinsWrappedLinesWithMatchingGeometry() {
        val result = grouper.group(listOf(
            line("The first line of a paragraph", y = 10f),
            line("continues naturally on the next line.", y = 24f)
        ))
        assertEquals(listOf("The first line of a paragraph continues naturally on the next line."), result)
    }

    @Test
    fun separatesParagraphsWhenVerticalGapIsLarge() {
        val result = grouper.group(listOf(
            line("First paragraph.", y = 10f),
            line("Second paragraph.", y = 34f)
        ))
        assertEquals(listOf("First paragraph.", "Second paragraph."), result)
    }

    @Test
    fun separatesHeadingWithDifferentFontSize() {
        val result = grouper.group(listOf(
            line("Chapter One", y = 10f, font = 20f),
            line("The story begins here.", y = 36f, font = 10f)
        ))
        assertEquals(listOf("Chapter One", "The story begins here."), result)
    }

    @Test
    fun separatesIndentedListItems() {
        val result = grouper.group(listOf(
            line("First item", y = 10f),
            line("• Second item", y = 24f)
        ))
        assertEquals(listOf("First item", "• Second item"), result)
    }

    @Test
    fun ignoresBlankLines() {
        val result = grouper.group(listOf(
            line("First line", y = 10f),
            line("   ", y = 24f),
            line("Continuation", y = 24f)
        ))
        assertEquals(listOf("First line Continuation"), result)
    }

    private fun line(
        text: String,
        y: Float,
        font: Float = 10f
    ) = PositionedLine(
        text = text,
        xStart = 40f,
        yTop = y,
        lineHeight = 10f,
        fontSize = font,
        pageWidth = 600f
    )
}
