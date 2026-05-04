package com.darknote.core.model

import kotlinx.serialization.Serializable

/**
 * Metadata for sync and usage tracking.
 * Separate from Snippet to avoid frequent updates to main entity.
 */
@Serializable
data class SnippetMetadata(
    val snippetId: String,
    val usageCount: Int = 0,           // For "most used" feature
    val lastCopiedAt: Long? = null,    // Last time copied to clipboard
    val dropboxRev: String? = null,    // Dropbox revision for sync
    val localHash: String,             // Content hash for change detection
    val lastSyncAt: Long? = null,
    val conflictStatus: ConflictStatus? = null
)

enum class ConflictStatus {
    LOCAL_WINS,
    REMOTE_WINS,
    MERGE_NEEDED
}
