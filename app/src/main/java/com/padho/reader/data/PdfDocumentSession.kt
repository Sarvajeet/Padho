package com.padho.reader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable

/**
 * Holds one open PdfRenderer for the active document.
 *
 * PdfRenderer is not thread-safe, so all access and document switches are
 * serialized. Switching to another URI closes the previous descriptor.
 */
class PdfDocumentSession(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private var activeUri: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    @Synchronized
    fun pageCount(uri: Uri): Int {
        ensureOpen(uri)
        return renderer?.pageCount ?: 0
    }

    @Synchronized
    fun renderPage(uri: Uri, pageIndex: Int, targetWidth: Int = 1200): Bitmap? {
        ensureOpen(uri)
        val current = renderer ?: return null
        if (pageIndex !in 0 until current.pageCount) return null

        current.openPage(pageIndex).use { page ->
            val width = targetWidth.coerceAtLeast(1)
            val height = (width.toFloat() * page.height / page.width)
                .toInt()
                .coerceAtLeast(1)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    private fun ensureOpen(uri: Uri) {
        if (activeUri == uri && renderer != null) return
        closeCurrent()

        val openedDescriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Unable to open this PDF.")
        try {
            val openedRenderer = PdfRenderer(openedDescriptor)
            descriptor = openedDescriptor
            renderer = openedRenderer
            activeUri = uri
        } catch (error: Exception) {
            openedDescriptor.close()
            throw error
        }
    }

    private fun closeCurrent() {
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
        activeUri = null
    }

    @Synchronized
    override fun close() {
        closeCurrent()
    }
}
