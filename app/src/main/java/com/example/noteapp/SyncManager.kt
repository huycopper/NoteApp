package com.example.noteapp

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager class to handle synchronization between local SQLite and Supabase
 */
class SyncManager(private val context: Context) {
    
    private val db = NotesDatabaseHelper(context)
    private val supabase = SupabaseClientManager.client
    
    companion object {
        private const val TAG = "SyncManager"
        private const val TABLE_NOTES = "notes"
    }
    
    /**
     * Check if user is currently logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return try {
            supabase.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status: ${e.message}")
            false
        }
    }
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID: ${e.message}")
            null
        }
    }
    
    /**
     * Perform full sync: upload local changes and download cloud changes
     */
    suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext SyncResult.NoNetwork
        }
        
        val userId = getCurrentUserId() ?: return@withContext SyncResult.NotLoggedIn
        
        try {
            //Upload unsynced notes
            uploadUnsyncedNotes(userId)
            //Delete notes marked for deletion on cloud
            deleteNotesFromCloud(userId)
            //Download notes from cloud
            downloadNotesFromCloud(userId)
            
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Upload all unsynced local notes to Supabase
     */
    private suspend fun uploadUnsyncedNotes(userId: String) {
        val unsyncedNotes = db.getUnsyncedNotes()
        
        for (note in unsyncedNotes) {
            if (note.isDeleted) continue  // Skip deleted notes, they will be handled separately
            
            try {
                if (note.supabaseId != null) {
                    // Update existing note on cloud
                    val supabaseNote = SupabaseNote(
                        id = note.supabaseId,
                        localId = note.id,
                        userId = userId,
                        title = note.title,
                        content = note.content,
                        isDeleted = false
                    )
                    
                    supabase.postgrest[TABLE_NOTES]
                        .update(supabaseNote) {
                            filter {
                                eq("id", note.supabaseId)
                            }
                        }
                    
                    db.markAsSynced(note.id, note.supabaseId)
                    Log.d(TAG, "Updated note on cloud: ${note.id}")
                } else {
                    // Insert new note to cloud
                    val supabaseNote = SupabaseNote(
                        localId = note.id,
                        userId = userId,
                        title = note.title,
                        content = note.content,
                        isDeleted = false
                    )
                    
                    val result = supabase.postgrest[TABLE_NOTES]
                        .insert(supabaseNote) {
                            select(Columns.ALL)
                        }
                        .decodeSingle<SupabaseNote>()
                    
                    result.id?.let { supabaseId ->
                        db.markAsSynced(note.id, supabaseId)
                        Log.d(TAG, "Inserted note to cloud: ${note.id} -> $supabaseId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload note ${note.id}: ${e.message}")
            }
        }
    }
    
    /**
     * Delete notes from cloud that were deleted locally
     */
    private suspend fun deleteNotesFromCloud(userId: String) {
        val deletedNotes = db.getDeletedNotes()
        
        for (note in deletedNotes) {
            try {
                note.supabaseId?.let { supabaseId ->
                    supabase.postgrest[TABLE_NOTES]
                        .delete {
                            filter {
                                eq("id", supabaseId)
                                eq("user_id", userId)
                            }
                        }
                    
                    db.permanentlyDeleteNote(note.id)
                    Log.d(TAG, "Deleted note from cloud: $supabaseId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete note from cloud ${note.supabaseId}: ${e.message}")
            }
        }
    }
    
    /**
     * Download all notes from cloud for current user
     */
    private suspend fun downloadNotesFromCloud(userId: String) {
        try {
            val cloudNotes = supabase.postgrest[TABLE_NOTES]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_deleted", false)
                    }
                }
                .decodeList<SupabaseNote>()
            
            for (cloudNote in cloudNotes) {
                db.insertOrUpdateFromCloud(cloudNote, userId)
                Log.d(TAG, "Downloaded note from cloud: ${cloudNote.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download notes from cloud: ${e.message}")
            throw e
        }
    }
    
    /**
     * Assign current user ID to all existing local notes (for first-time login)
     */
    suspend fun assignUserToLocalNotes() {
        val userId = getCurrentUserId() ?: return
        db.setUserIdForAllNotes(userId)
        Log.d(TAG, "Assigned user $userId to all local notes")
    }
}

/**
 * Result of sync operation
 */
sealed class SyncResult {
    object Success : SyncResult()
    object NoNetwork : SyncResult()
    object NotLoggedIn : SyncResult()
    data class Error(val message: String) : SyncResult()
}
