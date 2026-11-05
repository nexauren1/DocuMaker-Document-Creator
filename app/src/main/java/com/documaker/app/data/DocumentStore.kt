package com.documaker.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Offline-first document storage. The editor writes small HTML snapshots while typing. */
data class DocItem(
    val id: Long,
    val title: String,
    val html: String,
    val createdAt: Long,
    val updatedAt: Long,
    val starred: Boolean = false,
    val folder: String = "My documents",
    val offline: Boolean = true
)

class DocumentStore(context: Context) : SQLiteOpenHelper(context, "documaker.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                html TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                starred INTEGER NOT NULL DEFAULT 0,
                folder TEXT NOT NULL DEFAULT 'My documents',
                offline INTEGER NOT NULL DEFAULT 1
            )""".trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versioned migrations will be added here as the data model evolves.
    }

    fun all(): List<DocItem> {
        val result = mutableListOf<DocItem>()
        readableDatabase.query("documents", null, null, null, null, null, "updated_at DESC").use { c ->
            while (c.moveToNext()) result += c.toDoc()
        }
        return result
    }

    fun get(id: Long): DocItem? = readableDatabase.query(
        "documents", null, "id=?", arrayOf(id.toString()), null, null, null
    ).use { c -> if (c.moveToFirst()) c.toDoc() else null }

    fun create(title: String, html: String, folder: String = "My documents"): DocItem {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title.ifBlank { "Untitled document" })
            put("html", html)
            put("created_at", now)
            put("updated_at", now)
            put("folder", folder)
            put("offline", 1)
        }
        val id = writableDatabase.insertOrThrow("documents", null, values)
        return get(id) ?: DocItem(id, title, html, now, now, folder = folder)
    }

    fun update(id: Long, title: String, html: String) {
        val values = ContentValues().apply {
            put("title", title.ifBlank { "Untitled document" })
            put("html", html)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun rename(id: Long, title: String) {
        val values = ContentValues().apply {
            put("title", title.ifBlank { "Untitled document" })
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun toggleStar(id: Long, starred: Boolean) {
        val values = ContentValues().apply { put("starred", if (starred) 1 else 0) }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun duplicate(id: Long): DocItem? {
        val source = get(id) ?: return null
        return create("${source.title} (copy)", source.html, source.folder)
    }

    fun delete(id: Long) {
        writableDatabase.delete("documents", "id=?", arrayOf(id.toString()))
    }

    fun move(id: Long, folder: String) {
        val values = ContentValues().apply { put("folder", folder) }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    private fun android.database.Cursor.toDoc() = DocItem(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        html = getString(getColumnIndexOrThrow("html")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        starred = getInt(getColumnIndexOrThrow("starred")) == 1,
        folder = getString(getColumnIndexOrThrow("folder")),
        offline = getInt(getColumnIndexOrThrow("offline")) == 1
    )
}

object DocumentTemplates {
    val items = listOf(
        "Blank document" to "<h1>Untitled document</h1><p>Start writing here…</p>",
        "Meeting notes" to "<h1>Meeting notes</h1><p><b>Date:</b> </p><p><b>Attendees:</b> </p><h2>Agenda</h2><ul><li></li></ul><h2>Discussion</h2><p></p><h2>Action items</h2><ul><li></li></ul>",
        "Business letter" to "<p>[Your name]</p><p>[Address]</p><p>[Date]</p><p></p><p>Dear [Recipient],</p><p></p><p>[Write your message here.]</p><p></p><p>Sincerely,</p><p>[Your name]</p>",
        "Project report" to "<h1>Project report</h1><p><b>Prepared by:</b> </p><p><b>Date:</b> </p><h2>Executive summary</h2><p></p><h2>Objectives</h2><ul><li></li></ul><h2>Progress</h2><p></p><h2>Next steps</h2><p></p>",
        "Resume" to "<h1>Your Name</h1><p>[Email] · [Phone] · [Location]</p><h2>Professional summary</h2><p></p><h2>Experience</h2><p><b>Role · Company</b></p><p>[Dates]</p><ul><li>Achievement or responsibility</li></ul><h2>Education</h2><p></p><h2>Skills</h2><p></p>",
        "Invoice" to "<h1>INVOICE</h1><p><b>Invoice no.:</b> </p><p><b>Date:</b> </p><p><b>Bill to:</b> </p><table><tr><th>Description</th><th>Qty</th><th>Unit price</th><th>Total</th></tr><tr><td></td><td></td><td></td><td></td></tr></table><p><b>Total:</b> </p><p>Thank you for your business.</p>",
        "Study notes" to "<h1>Study notes</h1><p><b>Subject:</b> </p><h2>Key concepts</h2><ul><li></li></ul><h2>Definitions</h2><p></p><h2>Questions</h2><ol><li></li></ol>"
    )
}