package com.example.noteapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Local Note model for SQLite database
 */
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val supabaseId: String? = null,  // UUID from Supabase
    val userId: String? = null,       // User ID from Supabase Auth
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

/**
 * Supabase Note model for cloud sync
 * Uses kotlinx.serialization for JSON parsing
 */
@Serializable //
data class SupabaseNote(
    val id: String? = null,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val title: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
)
