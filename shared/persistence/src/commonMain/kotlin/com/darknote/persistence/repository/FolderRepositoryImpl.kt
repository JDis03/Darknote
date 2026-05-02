package com.darknote.persistence.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.darknote.core.model.Folder
import com.darknote.core.repository.FolderRepository
import com.darknote.persistence.database.DarkNoteDatabase
import com.darknote.persistence.database.Folder as DbFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FolderRepositoryImpl(
    private val database: DarkNoteDatabase
) : FolderRepository {

    private val queries = database.snippetQueries

    override fun getAll(): Flow<List<Folder>> {
        return queries.selectAllFolders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toFolder() } }
    }

    override fun getRootFolders(): Flow<List<Folder>> {
        return queries.selectRootFolders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toFolder() } }
    }

    override fun getSubfolders(parentId: String): Flow<List<Folder>> {
        return queries.selectSubfolders(parentId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toFolder() } }
    }

    override suspend fun getById(id: String): Folder? {
        return withContext(Dispatchers.Default) {
            queries.selectFolderById(id).executeAsOneOrNull()?.toFolder()
        }
    }

    override suspend fun create(folder: Folder): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.insertFolder(
                    id = folder.id,
                    name = folder.name,
                    parent_id = folder.parentId,
                    sort_order = folder.sortOrder.toLong(),
                    created_at = folder.createdAt
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(folder: Folder): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                queries.updateFolder(
                    name = folder.name,
                    parent_id = folder.parentId,
                    sort_order = folder.sortOrder.toLong(),
                    id = folder.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: String, moveChildrenToParent: Boolean): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                if (moveChildrenToParent) {
                    // Get folder to find its parent
                    val folder = queries.selectFolderById(id).executeAsOneOrNull()
                    val parentId = folder?.parent_id

                    // Move children to parent
                    queries.selectSubfolders(id).executeAsList().forEach { child ->
                        queries.updateFolder(
                            name = child.name,
                            parent_id = parentId,
                            sort_order = child.sort_order,
                            id = child.id
                        )
                    }
                }
                queries.deleteFolder(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun move(folderId: String, newParentId: String?): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val folder = queries.selectFolderById(folderId).executeAsOneOrNull()
                    ?: return@withContext Result.failure<Unit>(IllegalArgumentException("Folder not found"))

                queries.updateFolder(
                    name = folder.name,
                    parent_id = newParentId,
                    sort_order = folder.sort_order,
                    id = folderId
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DbFolder.toFolder(): Folder {
        return Folder(
            id = id,
            name = name,
            parentId = parent_id,
            sortOrder = sort_order?.toInt() ?: 0,
            createdAt = created_at
        )
    }
}
