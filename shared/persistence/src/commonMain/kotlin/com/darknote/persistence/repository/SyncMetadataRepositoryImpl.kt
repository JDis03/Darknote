package com.darknote.persistence.repository

import com.darknote.core.model.SyncMetadata
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.persistence.database.DarkNoteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncMetadataRepositoryImpl(
    private val database: DarkNoteDatabase
) : SyncMetadataRepository {
    
    private val queries = database.snippetQueries
    
    override suspend fun getBySnippetId(snippetId: String): SyncMetadata? {
        return withContext(Dispatchers.Default) {
            // We'll store sync metadata in snippet_metadata table
            queries.selectMetadataBySnippetId(snippetId)
                .executeAsOneOrNull()
                ?.let { metadata ->
                    SyncMetadata(
                        snippetId = snippetId,
                        remoteRevision = metadata.dropbox_rev,
                        lastSyncTime = metadata.last_sync_at ?: 0L,
                        syncStatus = when (metadata.conflict_status) {
                            null -> SyncStatus.SYNCED
                            "conflict" -> SyncStatus.CONFLICT
                            "pending_upload" -> SyncStatus.PENDING_UPLOAD
                            "pending_download" -> SyncStatus.PENDING_DOWNLOAD
                            "error" -> SyncStatus.ERROR
                            "not_synced" -> SyncStatus.NOT_SYNCED
                            else -> SyncStatus.NOT_SYNCED
                        }
                    )
                }
        }
    }
    
    override suspend fun getAll(): List<SyncMetadata> {
        return withContext(Dispatchers.Default) {
            queries.selectAllSnippetMetadata()
                .executeAsList()
                .map { metadata ->
                    SyncMetadata(
                        snippetId = metadata.snippet_id,
                        remoteRevision = metadata.dropbox_rev,
                        lastSyncTime = metadata.last_sync_at ?: 0L,
                        syncStatus = when (metadata.conflict_status) {
                            null -> SyncStatus.SYNCED
                            "conflict" -> SyncStatus.CONFLICT
                            "pending_upload" -> SyncStatus.PENDING_UPLOAD
                            "pending_download" -> SyncStatus.PENDING_DOWNLOAD
                            "error" -> SyncStatus.ERROR
                            "not_synced" -> SyncStatus.NOT_SYNCED
                            else -> SyncStatus.NOT_SYNCED
                        }
                    )
                }
        }
    }
    
    override suspend fun save(metadata: SyncMetadata): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.insertOrReplaceMetadata(
                    snippet_id = metadata.snippetId,
                    usage_count = 0, // Default value
                    last_copied_at = null,
                    dropbox_rev = metadata.remoteRevision,
                    local_hash = "", // NOT NULL field, use empty string
                    last_sync_at = metadata.lastSyncTime,
                    conflict_status = when (metadata.syncStatus) {
                        SyncStatus.SYNCED -> null
                        SyncStatus.CONFLICT -> "conflict"
                        SyncStatus.PENDING_UPLOAD -> "pending_upload"
                        SyncStatus.PENDING_DOWNLOAD -> "pending_download"
                        SyncStatus.ERROR -> "error"
                        SyncStatus.NOT_SYNCED -> "not_synced"
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(snippetId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.deleteSnippetMetadata(snippetId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingSync(): List<SyncMetadata> {
        return withContext(Dispatchers.Default) {
            queries.selectAllSnippetMetadata()
                .executeAsList()
                .mapNotNull { metadata ->
                    when (metadata.conflict_status) {
                        "pending_upload", "pending_download", "error" -> {
                            SyncMetadata(
                                snippetId = metadata.snippet_id,
                                remoteRevision = metadata.dropbox_rev,
                                lastSyncTime = metadata.last_sync_at ?: 0L,
                                syncStatus = when (metadata.conflict_status) {
                                    "pending_upload" -> SyncStatus.PENDING_UPLOAD
                                    "pending_download" -> SyncStatus.PENDING_DOWNLOAD
                                    "error" -> SyncStatus.ERROR
                                    else -> SyncStatus.NOT_SYNCED
                                }
                            )
                        }
                        else -> null
                    }
                }
        }
    }
    
    override suspend fun updateRemoteRevision(snippetId: String, revision: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                // Get existing metadata first
                val existing = queries.selectMetadataBySnippetId(snippetId).executeAsOneOrNull()
                if (existing != null) {
                    // Update using replace
                    queries.insertOrReplaceMetadata(
                        snippet_id = snippetId,
                        usage_count = existing.usage_count ?: 0,
                        last_copied_at = existing.last_copied_at,
                        dropbox_rev = revision,
                        local_hash = existing.local_hash ?: "", // Handle nullable field
                        last_sync_at = existing.last_sync_at,
                        conflict_status = existing.conflict_status
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateLastSyncTime(snippetId: String, timestamp: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                // Get existing metadata first
                val existing = queries.selectMetadataBySnippetId(snippetId).executeAsOneOrNull()
                if (existing != null) {
                    // Update using replace
                    queries.insertOrReplaceMetadata(
                        snippet_id = snippetId,
                        usage_count = existing.usage_count ?: 0,
                        last_copied_at = existing.last_copied_at,
                        dropbox_rev = existing.dropbox_rev,
                        local_hash = existing.local_hash ?: "", // Handle nullable field
                        last_sync_at = timestamp,
                        conflict_status = existing.conflict_status
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}