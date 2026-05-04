package com.darknote.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a code snippet or command that can be copied to clipboard.
 * Text is always stored plain, never formatted.
 */
@Serializable
data class Snippet(
    val id: String,
    val title: String,
    val content: String,         // Plain text only, no formatting
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val language: String? = null, // "bash", "python", "kotlin", "config", etc.
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val modifiedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val localPath: String,       // Path to .txt file
    val docPath: String? = null  // Optional path to .md documentation
) {
    companion object {
        const val FILE_EXTENSION = ".txt"
        const val DOC_EXTENSION = ".md"
    }
}

enum class SyncStatus {
    NOT_SYNCED,
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    CONFLICT,
    ERROR
}
