package com.darknote.android.di

import com.darknote.core.repository.DeletedSnippetRepository
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.DropboxClientFactory
import com.darknote.sync.engine.SyncEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideDropboxClient(): DropboxClient = DropboxClientFactory.create()

    @Provides
    @Singleton
    fun provideSyncEngine(
        dropboxClient: DropboxClient,
        snippetRepository: SnippetRepository,
        folderRepository: FolderRepository,
        syncMetadataRepository: SyncMetadataRepository,
        deletedSnippetRepository: DeletedSnippetRepository,
        storageService: FileStorageService
    ): SyncEngine = SyncEngine(
        dropboxClient = dropboxClient,
        snippetRepository = snippetRepository,
        folderRepository = folderRepository,
        syncMetadataRepository = syncMetadataRepository,
        deletedSnippetRepository = deletedSnippetRepository,
        storageService = storageService
    )
}
