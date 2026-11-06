package com.documaker.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

data class DocumentVersion(
    val id: Long,
    val documentId: Long,
    val title: String,
    val html: String,
    val createdAt: Long
)

/**
 * Local-first document store.
 *
 * Autosave writes the current document immediately. History snapshots are throttled
 * so that a user typing continuously does not create hundreds of database rows.
 */
class DocumentStore(context: Context) : SQLiteOpenHelper(context, "documaker.db", null, 2) {
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
        createVersionsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createVersionsTable(db)
    }

    private fun createVersionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS document_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                document_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                html TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_versions_document_time ON document_versions(document_id, created_at DESC)")
    }

    fun all(): List<DocItem> {
        val result = mutableListOf<DocItem>()
        readableDatabase.query("documents", null, null, null, null, null, "updated_at DESC").use { cursor ->
            while (cursor.moveToNext()) result += cursor.toDoc()
        }
        return result
    }

    fun search(query: String): List<DocItem> {
        val q = query.trim()
        if (q.isEmpty()) return all()
        val result = mutableListOf<DocItem>()
        readableDatabase.query(
            "documents", null, "title LIKE ? OR html LIKE ?",
            arrayOf("%$q%", "%$q%"), null, null, "updated_at DESC"
        ).use { cursor -> while (cursor.moveToNext()) result += cursor.toDoc() }
        return result
    }

    fun get(id: Long): DocItem? = readableDatabase.query(
        "documents", null, "id=?", arrayOf(id.toString()), null, null, null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toDoc() else null }

    fun create(title: String, html: String, folder: String = "My documents"): DocItem {
        val now = System.currentTimeMillis()
        val safeTitle = title.trim().ifBlank { "Untitled document" }
        val values = ContentValues().apply {
            put("title", safeTitle)
            put("html", html)
            put("created_at", now)
            put("updated_at", now)
            put("folder", folder.trim().ifBlank { "My documents" })
            put("offline", 1)
        }
        val db = writableDatabase
        val id = db.insertOrThrow("documents", null, values)
        addVersion(db, id, safeTitle, html, now)
        return get(id) ?: DocItem(id, safeTitle, html, now, now, folder = folder)
    }

    fun update(id: Long, title: String, html: String) {
        val current = get(id) ?: return
        val safeTitle = title.trim().ifBlank { "Untitled document" }
        if (current.title == safeTitle && current.html == html) return
        val now = System.currentTimeMillis()
        val db = writableDatabase
        val lastVersionAt = db.rawQuery(
            "SELECT created_at FROM document_versions WHERE document_id=? ORDER BY created_at DESC LIMIT 1",
            arrayOf(id.toString())
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        if (current.html != html && (lastVersionAt == 0L || now - lastVersionAt >= 60_000L)) {
            addVersion(db, id, current.title, current.html, now)
            db.execSQL(
                "DELETE FROM document_versions WHERE document_id=? AND id NOT IN (SELECT id FROM document_versions WHERE document_id=? ORDER BY created_at DESC LIMIT 40)",
                arrayOf(id, id)
            )
        }
        val values = ContentValues().apply {
            put("title", safeTitle)
            put("html", html)
            put("updated_at", now)
        }
        db.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun rename(id: Long, title: String) {
        val current = get(id) ?: return
        val safeTitle = title.trim().ifBlank { "Untitled document" }
        if (current.title == safeTitle) return
        update(id, safeTitle, current.html)
    }

    fun toggleStar(id: Long, starred: Boolean) {
        val values = ContentValues().apply { put("starred", if (starred) 1 else 0) }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun setOffline(id: Long, offline: Boolean) {
        val values = ContentValues().apply { put("offline", if (offline) 1 else 0) }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun duplicate(id: Long): DocItem? {
        val source = get(id) ?: return null
        return create("${source.title} (copy)", source.html, source.folder)
    }

    fun delete(id: Long) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("document_versions", "document_id=?", arrayOf(id.toString()))
            writableDatabase.delete("documents", "id=?", arrayOf(id.toString()))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    fun move(id: Long, folder: String) {
        val values = ContentValues().apply { put("folder", folder.trim().ifBlank { "My documents" }) }
        writableDatabase.update("documents", values, "id=?", arrayOf(id.toString()))
    }

    fun versions(documentId: Long): List<DocumentVersion> {
        val result = mutableListOf<DocumentVersion>()
        readableDatabase.query(
            "document_versions", null, "document_id=?", arrayOf(documentId.toString()),
            null, null, "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += DocumentVersion(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                documentId = cursor.getLong(cursor.getColumnIndexOrThrow("document_id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                html = cursor.getString(cursor.getColumnIndexOrThrow("html")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        }
        return result
    }

    fun restoreVersion(documentId: Long, versionId: Long): DocItem? {
        val version = readableDatabase.query(
            "document_versions", null, "id=? AND document_id=?",
            arrayOf(versionId.toString(), documentId.toString()), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else DocumentVersion(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                documentId = cursor.getLong(cursor.getColumnIndexOrThrow("document_id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                html = cursor.getString(cursor.getColumnIndexOrThrow("html")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        } ?: return null
        val current = get(documentId) ?: return null
        val db = writableDatabase
        addVersion(db, documentId, current.title, current.html, System.currentTimeMillis())
        val values = ContentValues().apply {
            put("title", version.title)
            put("html", version.html)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("documents", values, "id=?", arrayOf(documentId.toString()))
        return get(documentId)
    }

    private fun addVersion(db: SQLiteDatabase, documentId: Long, title: String, html: String, timestamp: Long) {
        val values = ContentValues().apply {
            put("document_id", documentId)
            put("title", title)
            put("html", html)
            put("created_at", timestamp)
        }
        db.insert("document_versions", null, values)
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
        "Blank document" to "<p></p>",
        "Meeting notes" to "<h1>Meeting notes</h1><p><b>Date:</b> </p><p><b>Attendees:</b> </p><h2>Agenda</h2><ul><li></li></ul><h2>Discussion</h2><p></p><h2>Decisions</h2><ul><li></li></ul><h2>Action items</h2><ul><li></li></ul>",
        "Business letter" to "<p>[Your name]</p><p>[Address]</p><p>[Date]</p><p></p><p>Dear [Recipient],</p><p></p><p>[Write your message here.]</p><p></p><p>Sincerely,</p><p>[Your name]</p>",
        "Project report" to "<h1>Project report</h1><p><b>Prepared by:</b> </p><p><b>Date:</b> </p><h2>Executive summary</h2><p></p><h2>Objectives</h2><ul><li></li></ul><h2>Progress</h2><p></p><h2>Risks</h2><p></p><h2>Next steps</h2><p></p>",
        "Resume" to "<h1>Your Name</h1><p>[Email] · [Phone] · [Location]</p><hr><h2>Professional summary</h2><p></p><h2>Experience</h2><p><b>Role · Company</b></p><p>[Dates]</p><ul><li>Achievement or responsibility</li></ul><h2>Education</h2><p></p><h2>Skills</h2><p></p>",
        "Invoice" to "<h1>INVOICE</h1><p><b>Invoice no.:</b> </p><p><b>Date:</b> </p><p><b>Bill to:</b> </p><table><tr><th>Description</th><th>Qty</th><th>Unit price</th><th>Total</th></tr><tr><td></td><td></td><td></td><td></td></tr></table><p><b>Total:</b> </p><p>Thank you for your business.</p>",
        "Study notes" to "<h1>Study notes</h1><p><b>Subject:</b> </p><h2>Learning goals</h2><ul><li></li></ul><h2>Key concepts</h2><ul><li></li></ul><h2>Definitions</h2><p></p><h2>Questions</h2><ol><li></li></ol>",
        "Academic essay" to "<h1>Essay title</h1><p><b>Student:</b> </p><p><b>Course:</b> </p><h2>Introduction</h2><p></p><h2>Argument</h2><p></p><h2>Evidence</h2><p></p><h2>Conclusion</h2><p></p><h2>References</h2><p></p>",
        "Lesson plan" to "<h1>Lesson plan</h1><p><b>Subject:</b> </p><p><b>Class:</b> </p><p><b>Duration:</b> </p><h2>Objectives</h2><ul><li></li></ul><h2>Materials</h2><ul><li></li></ul><h2>Activities</h2><ol><li></li></ol><h2>Assessment</h2><p></p>",
        "Project proposal" to "<h1>Project proposal</h1><p><b>Prepared by:</b> </p><p><b>Date:</b> </p><h2>Problem</h2><p></p><h2>Solution</h2><p></p><h2>Timeline</h2><p></p><h2>Budget</h2><p></p><h2>Expected results</h2><p></p>",
        "Book chapter" to "<h1>Chapter title</h1><p></p><h2>Section 1</h2><p></p><h2>Section 2</h2><p></p><h2>Notes</h2><p></p>"
    )
}
