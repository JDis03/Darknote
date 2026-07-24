package com.darknote.android.di

import android.content.Context
import com.darknote.core.backup.BackupService
import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.android.clipboard.AndroidClipboardManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideFileStorageService(@ApplicationContext context: Context): FileStorageService =
        FileStorageService(context.filesDir)

    @Provides
    @Singleton
    fun provideClipboardManager(@ApplicationContext context: Context): ClipboardManager {
        val sanitizer = ClipboardSanitizer(ClipboardSettings.DEFAULT)
        return AndroidClipboardManager(sanitizer, context)
    }

    @Provides
    @Singleton
    fun provideBackupService(
        snippetRepository: SnippetRepository,
        folderRepository: FolderRepository,
        fileStorageService: FileStorageService
    ): BackupService = BackupService(snippetRepository, folderRepository, fileStorageService)
}
