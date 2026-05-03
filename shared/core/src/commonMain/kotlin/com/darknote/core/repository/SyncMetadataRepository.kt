package com.darknote.core.repository

import com.darknote.core.model.SyncMetadata

/**
 * Repository for managing sync metadata.
 */
interface SyncMetadataRepository {
    
    /**
     * Get sync metadata for a snippet.
     */
    suspend fun getBySnippetId(snippetId: String): SyncMetadata?
    
    /**
     * Get all sync metadata.
     */
    suspend fun getAll(): List<SyncMetadata>
    
    /**
     * Store or update sync metadata.
     */
    suspend fun save(metadata: SyncMetadata): Result<Unit>
    
    /**
     * Delete sync metadata.
     */
    suspend fun delete(snippetId: String): Result<Unit>
    
    /**
     * Get snippets that need to be synced.
     */
    suspend fun getPendingSync(): List<SyncMetadata>
    
    /**
     * Update remote revision for a snippet.
     */
    suspend fun updateRemoteRevision(snippetId: String, revision: String): Result<Unit>
    
    /**
     * Update last sync time for a snippet.
     */
    suspend fun updateLastSyncTime(snippetId: String, timestamp: Long = System.currentTimeMillis()): Result<Unit>
}