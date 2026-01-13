package com.example.noteapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotesDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "notesapp.db"
        private const val DATABASE_VERSION = 2  // Upgraded for sync support
        private const val TABLE_NAME = "allnotes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_SUPABASE_ID = "supabase_id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_IS_SYNCED = "is_synced"
        private const val COLUMN_IS_DELETED = "is_deleted"
    }

    override fun onCreate(db: SQLiteDatabase?) { // Dùng để tạo bảng
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_SUPABASE_ID TEXT,
                $COLUMN_USER_ID TEXT,
                $COLUMN_UPDATED_AT INTEGER DEFAULT 0,
                $COLUMN_IS_SYNCED INTEGER DEFAULT 0,
                $COLUMN_IS_DELETED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add new columns for sync support
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_SUPABASE_ID TEXT")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_USER_ID TEXT")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_UPDATED_AT INTEGER DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_SYNCED INTEGER DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_DELETED INTEGER DEFAULT 0")
        }
    }

    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_SUPABASE_ID, note.supabaseId)
            put(COLUMN_USER_ID, note.userId)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            put(COLUMN_IS_SYNCED, if (note.isSynced) 1 else 0)
            put(COLUMN_IS_DELETED, if (note.isDeleted) 1 else 0)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllNotes(): List<Note> { // lấy các ghi chú từ sqlite vào ứng dụng
        val notesList = mutableListOf<Note>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_DELETED = 0"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val note = cursorToNote(cursor)
            notesList.add(note)
        }
        cursor.close()
        db.close()
        return notesList
    }

    fun getUnsyncedNotes(): List<Note> {
        val notesList = mutableListOf<Note>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_SYNCED = 0"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val note = cursorToNote(cursor)
            notesList.add(note)
        }
        cursor.close()
        db.close()
        return notesList
    }

    fun getDeletedNotes(): List<Note> {
        val notesList = mutableListOf<Note>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_DELETED = 1 AND $COLUMN_SUPABASE_ID IS NOT NULL"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val note = cursorToNote(cursor)
            notesList.add(note)
        }
        cursor.close()
        db.close()
        return notesList
    }

    private fun cursorToNote(cursor: android.database.Cursor): Note {
        val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
        val titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
        val contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT)
        val supabaseIdIndex = cursor.getColumnIndex(COLUMN_SUPABASE_ID)
        val userIdIndex = cursor.getColumnIndex(COLUMN_USER_ID)
        val updatedAtIndex = cursor.getColumnIndex(COLUMN_UPDATED_AT)
        val isSyncedIndex = cursor.getColumnIndex(COLUMN_IS_SYNCED)
        val isDeletedIndex = cursor.getColumnIndex(COLUMN_IS_DELETED)

        return Note(
            id = cursor.getInt(idIndex),
            title = cursor.getString(titleIndex),
            content = cursor.getString(contentIndex),
            supabaseId = if (supabaseIdIndex >= 0) cursor.getString(supabaseIdIndex) else null,
            userId = if (userIdIndex >= 0) cursor.getString(userIdIndex) else null,
            updatedAt = if (updatedAtIndex >= 0) cursor.getLong(updatedAtIndex) else 0L,
            isSynced = if (isSyncedIndex >= 0) cursor.getInt(isSyncedIndex) == 1 else false,
            isDeleted = if (isDeletedIndex >= 0) cursor.getInt(isDeletedIndex) == 1 else false
        )
    }

    fun updateNote(note: Note) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_SUPABASE_ID, note.supabaseId)
            put(COLUMN_USER_ID, note.userId)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            put(COLUMN_IS_SYNCED, 0)  // Mark as unsynced after update
            put(COLUMN_IS_DELETED, if (note.isDeleted) 1 else 0)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(note.id.toString())
        db.update(TABLE_NAME, values, whereClause, whereArgs)
        db.close()
    }

    fun markAsSynced(noteId: Int, supabaseId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SUPABASE_ID, supabaseId)
            put(COLUMN_IS_SYNCED, 1)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(noteId.toString())
        db.update(TABLE_NAME, values, whereClause, whereArgs)
        db.close()
    }

    fun setUserIdForAllNotes(userId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_IS_SYNCED, 0)  // Mark as unsynced to trigger upload
        }
        db.update(TABLE_NAME, values, null, null)
        db.close()
    }

    fun getNoteById(noteId: Int): Note {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = $noteId"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()

        val note = cursorToNote(cursor)

        cursor.close()
        db.close()
        return note
    }

    fun getNoteBySupabaseId(supabaseId: String): Note? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_SUPABASE_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(supabaseId))
        
        val note = if (cursor.moveToFirst()) cursorToNote(cursor) else null
        
        cursor.close()
        db.close()
        return note
    }

    fun deleteNote(noteId: Int) {
        val db = writableDatabase
        // Soft delete - mark as deleted for sync
        val note = getNoteById(noteId)
        if (note.supabaseId != null) {
            // If synced before, mark as deleted for cloud sync
            val values = ContentValues().apply {
                put(COLUMN_IS_DELETED, 1)
                put(COLUMN_IS_SYNCED, 0)
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(noteId.toString()))
        } else {
            // If never synced, just delete locally
            db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(noteId.toString()))
        }
        db.close()
    }

    fun permanentlyDeleteNote(noteId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(noteId.toString()))
        db.close()
    }

    fun insertOrUpdateFromCloud(supabaseNote: SupabaseNote, userId: String): Long {
        val existingNote = supabaseNote.id?.let { getNoteBySupabaseId(it) }
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_TITLE, supabaseNote.title)
            put(COLUMN_CONTENT, supabaseNote.content)
            put(COLUMN_SUPABASE_ID, supabaseNote.id)
            put(COLUMN_USER_ID, userId)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
            put(COLUMN_IS_SYNCED, 1)
            put(COLUMN_IS_DELETED, if (supabaseNote.isDeleted) 1 else 0)
        }

        val result = if (existingNote != null) {
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(existingNote.id.toString()))
            existingNote.id.toLong()
        } else {
            db.insert(TABLE_NAME, null, values)
        }
        
        db.close()
        return result
    }
}