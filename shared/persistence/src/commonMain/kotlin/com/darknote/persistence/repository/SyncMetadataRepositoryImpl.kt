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
            queries.selectMetadataBySnippetId(snippetId)
                .executeAsOneOrNull()
                ?.toSyncMetadata()
        }
    }

    override suspend fun getAll(): List<SyncMetadata> {
        return withContext(Dispatchers.Default) {
            queries.selectAllSnippetMetadata()
                .executeAsList()
                .map { it.toSyncMetadata() }
        }
    }

    override suspend fun save(metadata: SyncMetadata): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.insertOrReplaceMetadata(
                    snippet_id = metadata.snippetId,
                    usage_count = 0,
                    last_copied_at = null,
                    dropbox_rev = metadata.remoteRevision,
                    last_sync_at = metadata.lastSyncTime,
                    sync_status = metadata.syncStatus.name.lowercase()
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
                .mapNotNull { db ->
                    val status = parseSyncStatus(db.sync_status)
                    if (status == SyncStatus.PENDING_UPLOAD ||
                        status == SyncStatus.PENDING_DOWNLOAD ||
                        status == SyncStatus.ERROR
                    ) {
                        db.toSyncMetadata().copy(syncStatus = status)
                    } else null
                }
        }
    }

    override suspend fun updateRemoteRevision(snippetId: String, revision: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val existing = queries.selectMetadataBySnippetId(snippetId).executeAsOneOrNull()
                queries.insertOrReplaceMetadata(
                    snippet_id = snippetId,
                    usage_count = existing?.usage_count ?: 0,
                    last_copied_at = existing?.last_copied_at,
                    dropbox_rev = revision,
                    last_sync_at = existing?.last_sync_at ?: System.currentTimeMillis(),
                    sync_status = existing?.sync_status
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSyncTime(snippetId: String, timestamp: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val existing = queries.selectMetadataBySnippetId(snippetId).executeAsOneOrNull()
                queries.insertOrReplaceMetadata(
                    snippet_id = snippetId,
                    usage_count = existing?.usage_count ?: 0,
                    last_copied_at = existing?.last_copied_at,
                    dropbox_rev = existing?.dropbox_rev,
                    last_sync_at = timestamp,
                    sync_status = existing?.sync_status
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.darknote.persistence.database.Snippet_metadata.toSyncMetadata(): SyncMetadata {
        return SyncMetadata(
            snippetId = snippet_id,
            remoteRevision = dropbox_rev,
            lastSyncTime = last_sync_at ?: 0L,
            syncStatus = parseSyncStatus(sync_status)
        )
    }

    private fun parseSyncStatus(raw: String?): SyncStatus = when (raw?.lowercase()) {
        null, "", "synced" -> SyncStatus.SYNCED
        "conflict" -> SyncStatus.CONFLICT
        "pending_upload" -> SyncStatus.PENDING_UPLOAD
        "pending_download" -> SyncStatus.PENDING_DOWNLOAD
        "error" -> SyncStatus.ERROR
        "not_synced" -> SyncStatus.NOT_SYNCED
        else -> runCatching { SyncStatus.valueOf(raw.uppercase()) }
            .getOrDefault(SyncStatus.NOT_SYNCED)
    }
}
