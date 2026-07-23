package com.darknote.core.model

/**
 * Metadata for tracking sync state of snippets.
 * Inspired by Joplin's synchronization tracking.
 */
data class SyncMetadata(
    val snippetId: String,
    val remoteRevision: String? = null,
    val lastSyncTime: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
)