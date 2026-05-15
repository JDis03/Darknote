package com.darknote.desktop.di

import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.desktop.platform.DesktopClipboardManager
import com.darknote.desktop.platform.DesktopFileStorageFactory
import com.darknote.desktop.viewmodel.SnippetListViewModel
import com.darknote.persistence.DatabaseDriverFactory
import com.darknote.persistence.database.DarkNoteDatabase
import com.darknote.persistence.repository.FolderRepositoryImpl
import com.darknote.persistence.repository.SnippetRepositoryImpl
import com.darknote.persistence.repository.SyncMetadataRepositoryImpl
import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.DropboxClientFactory
import com.darknote.sync.engine.SyncEngine
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val desktopModule = module {
    // Database
    single { DatabaseDriverFactory().createDriver() }
    single { DarkNoteDatabase(get()) }
    
    // Repositories
    single<SnippetRepository> { SnippetRepositoryImpl(get()) }
    single<FolderRepository> { FolderRepositoryImpl(get()) }
    single<SyncMetadataRepository> { SyncMetadataRepositoryImpl(get()) }
    
    // Storage & Platform
    single<FileStorageService> { DesktopFileStorageFactory.create() }
    single<ClipboardManager> { DesktopClipboardManager() }
    
    // Sync
    single<DropboxClient> { DropboxClientFactory.create() }
    single {
        SyncEngine(
            dropboxClient = get(),
            snippetRepository = get(),
            folderRepository = get(),
            syncMetadataRepository = get(),
            storageService = get()
        )
    }
    
    // ViewModels
    singleOf(::SnippetListViewModel)
}

fun initKoin(): List<Module> {
    return listOf(desktopModule)
}
