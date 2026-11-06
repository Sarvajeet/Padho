package com.padho.reader.data

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Runs on-device Latin-script OCR against a rendered PDF page.
 * The recognizer is scoped to one extraction so it is always closed.
 */
object OcrTextExtractor {

    fun extractText(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            result.textBlocks
                .flatMap { block -> block.lines }
                .map { line -> line.text.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .trim()
        } finally {
            recognizer.close()
        }
    }
}
