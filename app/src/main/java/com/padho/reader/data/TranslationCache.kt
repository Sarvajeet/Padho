package com.padho.reader.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Persistent cache for page translations.
 *
 * The cache key must include document identity, page, source/target language,
 * and translation engine version so stale translations are never reused.
 */
class TranslationCache(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSLATIONS (
                cache_key TEXT PRIMARY KEY NOT NULL,
                translated_text TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Preserve existing translations when adding future schema versions.
    }

    fun get(cacheKey: String): String? {
        readableDatabase.query(
            TABLE_TRANSLATIONS,
            arrayOf(COLUMN_TRANSLATED_TEXT),
            "$COLUMN_CACHE_KEY = ?",
            arrayOf(cacheKey),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    fun put(cacheKey: String, translatedText: String) {
        val values = ContentValues().apply {
            put(COLUMN_CACHE_KEY, cacheKey)
            put(COLUMN_TRANSLATED_TEXT, translatedText)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_TRANSLATIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        prune()
    }

    /**
     * Bound storage growth: expire old entries, then retain the newest entries
     * within both row-count and approximate text-size limits.
     */
    private fun prune() {
        val db = writableDatabase
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        db.delete(TABLE_TRANSLATIONS, "$COLUMN_UPDATED_AT < ?", arrayOf(cutoff.toString()))

        val staleKeys = mutableListOf<String>()
        var retainedRows = 0
        var retainedBytes = 0L
        db.query(
            TABLE_TRANSLATIONS,
            arrayOf(COLUMN_CACHE_KEY, "length($COLUMN_CACHE_KEY) + length($COLUMN_TRANSLATED_TEXT) AS entry_size"),
            null,
            null,
            null,
            null,
            "$COLUMN_UPDATED_AT DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val key = cursor.getString(0)
                val bytes = cursor.getLong(1)
                retainedRows++
                retainedBytes += bytes
                if (retainedRows > MAX_ENTRIES || retainedBytes > MAX_TEXT_BYTES) {
                    staleKeys += key
                    retainedRows--
                    retainedBytes -= bytes
                }
            }
        }
        db.beginTransaction()
        try {
            staleKeys.forEach { key ->
                db.delete(TABLE_TRANSLATIONS, "$COLUMN_CACHE_KEY = ?", arrayOf(key))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "padho_translations.db"
        private const val DATABASE_VERSION = 1
        private const val MAX_ENTRIES = 500
        private const val MAX_TEXT_BYTES = 32L * 1024L * 1024L
        private const val MAX_AGE_MS = 90L * 24L * 60L * 60L * 1000L
        private const val TABLE_TRANSLATIONS = "page_translations"
        private const val COLUMN_CACHE_KEY = "cache_key"
        private const val COLUMN_TRANSLATED_TEXT = "translated_text"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }
}
