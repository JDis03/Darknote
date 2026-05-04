package com.darknote.core.repository

import com.darknote.core.model.Snippet
import com.darknote.core.model.SnippetMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for snippet operations.
 * Abstracts data source (database, files) from domain layer.
 */
interface SnippetRepository {
    /**
     * Get all snippets as reactive flow.
     */
    fun getAll(): Flow<List<Snippet>>

    /**
     * Get snippets by folder ID.
     */
    fun getByFolder(folderId: String?): Flow<List<Snippet>>

    /**
     * Get snippet by ID.
     */
    suspend fun getById(id: String): Snippet?

    /**
     * Get favorite snippets.
     */
    fun getFavorites(): Flow<List<Snippet>>

    /**
     * Search snippets by query.
     */
    fun search(query: String): Flow<List<Snippet>>

    /**
     * Create new snippet.
     */
    suspend fun create(snippet: Snippet): Result<Unit>

    /**
     * Update existing snippet.
     */
    suspend fun update(snippet: Snippet): Result<Unit>

    /**
     * Delete snippet.
     */
    suspend fun delete(id: String): Result<Unit>

    /**
     * Get snippet metadata.
     */
    suspend fun getMetadata(snippetId: String): SnippetMetadata?

    /**
     * Update metadata (usage count, last copied, etc.).
     */
    suspend fun updateMetadata(metadata: SnippetMetadata): Result<Unit>

    /**
     * Increment usage count for a snippet.
     */
    suspend fun incrementUsageCount(snippetId: String): Result<Unit>
    
    /**
     * Get all snippets without reactive flow (for sync operations).
     */
    suspend fun getAllCached(): List<Snippet>
    
    /**
     * Get snippet by ID without reactive flow (for sync operations).
     */
    suspend fun getByIdCached(id: String): Snippet?
}
