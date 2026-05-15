package com.darknote.persistence.repository

import com.darknote.core.repository.DeletedSnippet
import com.darknote.core.repository.DeletedSnippetRepository
import com.darknote.persistence.database.DarkNoteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeletedSnippetRepositoryImpl(
    private val database: DarkNoteDatabase
) : DeletedSnippetRepository {

    private val queries = database.snippetQueries

    override suspend fun insert(id: String, deletedAt: Long) {
        withContext(Dispatchers.Default) {
            queries.insertDeletedSnippet(id, deletedAt)
        }
    }

    override suspend fun getAll(): List<DeletedSnippet> {
        return withContext(Dispatchers.Default) {
            queries.selectAllDeletedSnippets()
                .executeAsList()
                .map { DeletedSnippet(id = it.id, deletedAt = it.deleted_at) }
        }
    }

    override suspend fun getById(id: String): DeletedSnippet? {
        return withContext(Dispatchers.Default) {
            queries.selectDeletedSnippetById(id)
                .executeAsOneOrNull()
                ?.let { DeletedSnippet(id = it.id, deletedAt = it.deleted_at) }
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.Default) {
            queries.deleteDeletedSnippet(id)
        }
    }
}
