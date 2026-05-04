package com.darknote.persistence.database

import com.darknote.persistence.repository.FolderRepositoryImpl
import com.darknote.persistence.repository.SnippetRepositoryImpl
import com.darknote.persistence.repository.SyncMetadataRepositoryImpl
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.SyncMetadataRepository

class DatabaseFactory(
    private val driverFactory: DriverFactory
) {
    private val database: DarkNoteDatabase by lazy {
        DarkNoteDatabase(driverFactory.createDriver())
    }

    val snippetRepository: SnippetRepository by lazy {
        SnippetRepositoryImpl(database)
    }

    val folderRepository: FolderRepository by lazy {
        FolderRepositoryImpl(database)
    }

    val syncMetadataRepository: SyncMetadataRepository by lazy {
        SyncMetadataRepositoryImpl(database)
    }
}