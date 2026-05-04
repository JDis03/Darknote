package com.darknote.persistence.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.darknote.core.model.Snippet
import com.darknote.core.model.SnippetMetadata
import com.darknote.core.repository.SnippetRepository
import com.darknote.persistence.database.DarkNoteDatabase
import com.darknote.persistence.database.Snippet as DbSnippet
import com.darknote.persistence.database.Snippet_metadata as DbSnippetMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of SnippetRepository.
 */
class SnippetRepositoryImpl(
    private val database: DarkNoteDatabase
) : SnippetRepository {

    private val queries = database.snippetQueries

    override fun getAll(): Flow<List<Snippet>> {
        return queries.selectAllSnippets()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toSnippet() } }
    }

    override fun getByFolder(folderId: String?): Flow<List<Snippet>> {
        return queries.selectSnippetsByFolder(folderId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toSnippet() } }
    }

    override suspend fun getById(id: String): Snippet? {
        return withContext(Dispatchers.Default) {
            queries.selectSnippetById(id).executeAsOneOrNull()?.toSnippet()
        }
    }

    override fun getFavorites(): Flow<List<Snippet>> {
        return queries.selectFavoriteSnippets()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toSnippet() } }
    }

    override fun search(query: String): Flow<List<Snippet>> {
        return queries.searchSnippets(query, query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toSnippet() } }
    }

    override suspend fun create(snippet: Snippet): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.insertSnippet(
                    id = snippet.id,
                    title = snippet.title,
                    folder_id = snippet.folderId,
                    tags = snippet.tags.joinToString(","),
                    language = snippet.language,
                    is_favorite = snippet.isFavorite,
                    created_at = snippet.createdAt,
                    modified_at = snippet.modifiedAt,
                    sync_status = snippet.syncStatus.name,
                    local_path = snippet.localPath,
                    doc_path = snippet.docPath
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(snippet: Snippet): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.updateSnippet(
                    title = snippet.title,
                    folder_id = snippet.folderId,
                    tags = snippet.tags.joinToString(","),
                    language = snippet.language,
                    is_favorite = snippet.isFavorite,
                    modified_at = snippet.modifiedAt,
                    sync_status = snippet.syncStatus.name,
                    local_path = snippet.localPath,
                    doc_path = snippet.docPath,
                    id = snippet.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.deleteSnippet(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(snippetId: String): SnippetMetadata? {
        return withContext(Dispatchers.Default) {
            queries.selectMetadataBySnippetId(snippetId).executeAsOneOrNull()?.toMetadata()
        }
    }

    override suspend fun updateMetadata(metadata: SnippetMetadata): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.insertOrReplaceMetadata(
                    snippet_id = metadata.snippetId,
                    usage_count = metadata.usageCount.toLong(),
                    last_copied_at = metadata.lastCopiedAt,
                    dropbox_rev = metadata.dropboxRev,
                    local_hash = metadata.localHash,
                    last_sync_at = metadata.lastSyncAt,
                    conflict_status = metadata.conflictStatus?.name
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementUsageCount(snippetId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.incrementUsageCount(System.currentTimeMillis(), snippetId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions for mapping
    private fun DbSnippet.toSnippet(): Snippet {
        return Snippet(
            id = id,
            title = title,
            content = "", // Content loaded from file
            folderId = folder_id,
            tags = tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            language = language,
            isFavorite = is_favorite == true,
            createdAt = created_at,
            modifiedAt = modified_at,
            syncStatus = com.darknote.core.model.SyncStatus.valueOf(sync_status),
            localPath = local_path,
            docPath = doc_path
        )
    }

    private fun DbSnippetMetadata.toMetadata(): SnippetMetadata {
        return SnippetMetadata(
            snippetId = snippet_id,
            usageCount = usage_count?.toInt() ?: 0,
            lastCopiedAt = last_copied_at,
            dropboxRev = dropbox_rev,
            localHash = local_hash,
            lastSyncAt = last_sync_at,
            conflictStatus = conflict_status?.let { 
                com.darknote.core.model.ConflictStatus.valueOf(it) 
            }
        )
    }
    
    override suspend fun getAllCached(): List<Snippet> {
        return withContext(Dispatchers.Default) {
            queries.selectAllSnippets()
                .executeAsList()
                .map { it.toSnippet() }
        }
    }
    
    override suspend fun getByIdCached(id: String): Snippet? {
        return withContext(Dispatchers.Default) {
            queries.selectSnippetById(id)
                .executeAsOneOrNull()
                ?.toSnippet()
        }
    }
}
