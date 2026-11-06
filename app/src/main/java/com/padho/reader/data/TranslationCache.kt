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
    }

    companion object {
        private const val DATABASE_NAME = "padho_translations.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TRANSLATIONS = "page_translations"
        private const val COLUMN_CACHE_KEY = "cache_key"
        private const val COLUMN_TRANSLATED_TEXT = "translated_text"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }
}
