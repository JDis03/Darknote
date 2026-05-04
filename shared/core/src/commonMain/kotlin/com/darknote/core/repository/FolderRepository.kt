package com.darknote.core.repository

import com.darknote.core.model.Folder
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for folder operations.
 */
interface FolderRepository {
    /**
     * Get all folders as reactive flow.
     */
    fun getAll(): Flow<List<Folder>>

    /**
     * Get root folders (without parent).
     */
    fun getRootFolders(): Flow<List<Folder>>

    /**
     * Get subfolders by parent ID.
     */
    fun getSubfolders(parentId: String): Flow<List<Folder>>

    /**
     * Get folder by ID.
     */
    suspend fun getById(id: String): Folder?

    /**
     * Create new folder.
     */
    suspend fun create(folder: Folder): Result<Unit>

    /**
     * Update existing folder.
     */
    suspend fun update(folder: Folder): Result<Unit>

    /**
     * Delete folder and optionally move children to parent.
     */
    suspend fun delete(id: String, moveChildrenToParent: Boolean = true): Result<Unit>

    /**
     * Move folder to new parent.
     */
    suspend fun move(folderId: String, newParentId: String?): Result<Unit>
}
