package com.darknote.core.repository

import com.darknote.core.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for tag operations.
 */
interface TagRepository {
    /**
     * Get all tags as reactive flow.
     */
    fun getAll(): Flow<List<Tag>>

    /**
     * Get tag by ID.
     */
    suspend fun getById(id: String): Tag?

    /**
     * Get tag by name.
     */
    suspend fun getByName(name: String): Tag?

    /**
     * Create new tag.
     */
    suspend fun create(tag: Tag): Result<Unit>

    /**
     * Update existing tag.
     */
    suspend fun update(tag: Tag): Result<Unit>

    /**
     * Delete tag.
     */
    suspend fun delete(id: String): Result<Unit>
}
