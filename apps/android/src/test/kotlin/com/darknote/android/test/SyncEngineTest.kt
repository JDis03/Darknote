package com.darknote.android.test

import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncMetadata
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.RemoteFile
import com.darknote.sync.engine.SyncEngine
import com.darknote.sync.engine.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncEngineTest {

    private val dropboxClient = mockk<DropboxClient>(relaxed = true)
    private val snippetRepository = mockk<SnippetRepository>(relaxed = true)
    private val folderRepository = mockk<FolderRepository>(relaxed = true)
    private val syncMetadataRepository = mockk<SyncMetadataRepository>(relaxed = true)
    private val storageService = mockk<FileStorageService>(relaxed = true)

    private fun createEngine(): SyncEngine = SyncEngine(
        dropboxClient = dropboxClient,
        snippetRepository = snippetRepository,
        folderRepository = folderRepository,
        syncMetadataRepository = syncMetadataRepository,
        storageService = storageService
    )

    @Test
    fun `initial state is Idle`() = runTest {
        val engine = createEngine()
        assertTrue(engine.state.value is SyncState.Idle)
    }

    @Test
    fun `sync fails when not authenticated`() = runTest {
        every { dropboxClient.isAuthorized() } returns false
        val engine = createEngine()

        engine.sync()

        assertTrue(engine.state.value is SyncState.Error)
        val error = engine.state.value as SyncState.Error
        assertTrue(error.message.contains("Not authenticated"))
    }

    @Test
    fun `sync succeeds with no pending changes`() = runTest {
        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns emptyList()
        coEvery { syncMetadataRepository.getAll() } returns emptyList()
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success("")

        val engine = createEngine()
        engine.sync()

        assertTrue(engine.state.value is SyncState.Synced)
    }

    @Test
    fun `detects local creates as pending changes`() = runTest {
        val snippet = Snippet(
            id = "snip-1", title = "test.py", content = "print('hello')",
            localPath = "snippets/test_123.txt",
            createdAt = 1000L, modifiedAt = 2000L
        )

        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns listOf(snippet)
        coEvery { syncMetadataRepository.getAll() } returns emptyList()
        coEvery { syncMetadataRepository.getBySnippetId("snip-1") } returns null
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success(snippet.content)
        coEvery { folderRepository.getById(any()) } returns null
        coEvery { dropboxClient.uploadFile(any(), any()) } returns Result.success("rev-001")
        coEvery { syncMetadataRepository.updateRemoteRevision(any(), any()) } returns Result.success(Unit)
        coEvery { syncMetadataRepository.updateLastSyncTime(any(), any()) } returns Result.success(Unit)

        val engine = createEngine()
        engine.sync()

        coVerify { dropboxClient.uploadFile(any(), any()) }
        assertTrue(engine.state.value is SyncState.Synced)
    }

    @Test
    fun `detects locally updated snippets`() = runTest {
        val snippet = Snippet(
            id = "snip-2", title = "update.sql", content = "SELECT * FROM users",
            localPath = "snippets/update_456.txt",
            createdAt = 1000L, modifiedAt = 5000L
        )
        val metadata = SyncMetadata(
            snippetId = "snip-2",
            remoteRevision = "rev-old",
            lastSyncTime = 3000L,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED
        )

        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns listOf(snippet)
        coEvery { syncMetadataRepository.getAll() } returns listOf(metadata)
        coEvery { syncMetadataRepository.getBySnippetId("snip-2") } returns metadata
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success(snippet.content)
        coEvery { folderRepository.getById(any()) } returns null
        coEvery { dropboxClient.uploadFile(any(), any()) } returns Result.success("rev-002")
        coEvery { syncMetadataRepository.updateRemoteRevision(any(), any()) } returns Result.success(Unit)
        coEvery { syncMetadataRepository.updateLastSyncTime(any(), any()) } returns Result.success(Unit)

        val engine = createEngine()
        engine.sync()

        coVerify { dropboxClient.uploadFile(any(), any()) }
        assertTrue(engine.state.value is SyncState.Synced)
    }

    @Test
    fun `does not upload unchanged snippets`() = runTest {
        val snippet = Snippet(
            id = "snip-3", title = "config.ini", content = "[main]\nkey=value",
            localPath = "snippets/config_789.txt",
            createdAt = 1000L, modifiedAt = 2000L
        )
        val metadata = SyncMetadata(
            snippetId = "snip-3",
            remoteRevision = "rev-current",
            lastSyncTime = 5000L,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED
        )

        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns listOf(snippet)
        coEvery { syncMetadataRepository.getAll() } returns listOf(metadata)
        coEvery { syncMetadataRepository.getBySnippetId("snip-3") } returns metadata
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(
            listOf(
                RemoteFile(
                    path = "/darknote/snip-3.txt",
                    name = "snip-3.txt",
                    modifiedTime = 2000L,
                    size = 100,
                    rev = "rev-current"
                )
            )
        )
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success(snippet.content)

        val engine = createEngine()
        engine.sync()

        // upload should NOT be called since snippet hasn't changed since last sync
        coVerify(exactly = 0) { dropboxClient.uploadFile(any(), any<String>()) }
    }

    @Test
    fun `sync error state contains error message`() = runTest {
        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } throws RuntimeException("Database crashed")
        coEvery { syncMetadataRepository.getAll() } returns emptyList()
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success("")

        val engine = createEngine()
        engine.sync()

        assertTrue(engine.state.value is SyncState.Error)
    }

    @Test
    fun `logs are populated during sync`() = runTest {
        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns emptyList()
        coEvery { syncMetadataRepository.getAll() } returns emptyList()
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success("")

        val engine = createEngine()
        engine.sync()

        assertTrue(engine.logs.value.isNotEmpty())
        assertTrue(engine.logs.value.any { it.message.contains("Synchronization completed") })
    }

    @Test
    fun `detects local deletions`() = runTest {
        val metadata = SyncMetadata(
            snippetId = "deleted-snip",
            remoteRevision = "rev-old",
            lastSyncTime = 1000L,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED
        )

        every { dropboxClient.isAuthorized() } returns true
        coEvery { snippetRepository.getAllCached() } returns emptyList()
        coEvery { syncMetadataRepository.getAll() } returns listOf(metadata)
        coEvery { dropboxClient.listFiles("/darknote") } returns Result.success(emptyList())
        coEvery { storageService.saveSnippetContent(any()) } returns Result.success(Unit)
        coEvery { storageService.loadSnippetContent(any()) } returns Result.success("")
        coEvery { dropboxClient.deleteFile("/darknote/deleted-snip.txt") } returns Result.success(Unit)
        coEvery { syncMetadataRepository.delete("deleted-snip") } returns Result.success(Unit)

        val engine = createEngine()
        engine.sync()

        coVerify { dropboxClient.deleteFile("/darknote/deleted-snip.txt") }
        coVerify { syncMetadataRepository.delete("deleted-snip") }
    }
}
