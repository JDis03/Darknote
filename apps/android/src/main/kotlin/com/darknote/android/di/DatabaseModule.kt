package com.darknote.android.di

import android.content.Context
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.persistence.database.AndroidDriverFactory
import com.darknote.persistence.database.DatabaseFactory
import com.darknote.persistence.database.DriverFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDriverFactory(@ApplicationContext context: Context): DriverFactory =
        AndroidDriverFactory(context)

    @Provides
    @Singleton
    fun provideDatabaseFactory(driverFactory: DriverFactory): DatabaseFactory =
        DatabaseFactory(driverFactory)

    @Provides
    @Singleton
    fun provideSnippetRepository(databaseFactory: DatabaseFactory): SnippetRepository =
        databaseFactory.snippetRepository

    @Provides
    @Singleton
    fun provideFolderRepository(databaseFactory: DatabaseFactory): FolderRepository =
        databaseFactory.folderRepository

    @Provides
    @Singleton
    fun provideSyncMetadataRepository(databaseFactory: DatabaseFactory): SyncMetadataRepository =
        databaseFactory.syncMetadataRepository
}
