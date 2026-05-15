package com.darknote.core.repository

/**
 * Repository for tracking locally-deleted snippets (tombstones).
 *
 * When a snippet is deleted locally, a tombstone record is inserted here.
 * The sync engine reads this table to know which remote files to delete,
 * preventing them from being re-downloaded as "new remote files".
 *
 * Once the remote deletion is confirmed, the tombstone is removed.
 */
interface DeletedSnippetRepository {

    /**
     * Records a snippet as locally deleted.
     *
     * @param id The snippet ID
     * @param deletedAt Epoch ms of deletion time (defaults to now)
     */
    suspend fun insert(id: String, deletedAt: Long = System.currentTimeMillis())

    /**
     * Returns all pending tombstones (snippets deleted locally but not yet deleted from remote).
     */
    suspend fun getAll(): List<DeletedSnippet>

    /**
     * Returns the tombstone for a given ID, or null if not found.
     */
    suspend fun getById(id: String): DeletedSnippet?

    /**
     * Removes a tombstone after remote deletion is confirmed (or if remote wins the conflict).
     */
    suspend fun delete(id: String)
}

/**
 * Represents a locally-deleted snippet pending remote cleanup.
 */
data class DeletedSnippet(
    val id: String,
    val deletedAt: Long
)
