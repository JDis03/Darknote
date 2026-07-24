package com.darknote.core.backup

import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.model.SnippetMetadata
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** In-memory fake — no real database needed to test backup/restore logic. */
private class FakeSnippetRepository : SnippetRepository {
    private val store = MutableStateFlow<Map<String, Snippet>>(emptyMap())

    override fun getAll(): Flow<List<Snippet>> = store.map { it.values.toList() }
    override fun getByFolder(folderId: String?): Flow<List<Snippet>> =
        store.map { it.values.filter { s -> s.folderId == folderId } }
    override suspend fun getById(id: String): Snippet? = store.value[id]
    override fun getFavorites(): Flow<List<Snippet>> = store.map { it.values.filter { s -> s.isFavorite } }

    override suspend fun create(snippet: Snippet): Result<Unit> {
        store.value = store.value + (snippet.id to snippet)
        return Result.success(Unit)
    }

    override suspend fun update(snippet: Snippet): Result<Unit> {
        store.value = store.value + (snippet.id to snippet)
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        store.value = store.value - id
        return Result.success(Unit)
    }

    override suspend fun getMetadata(snippetId: String): SnippetMetadata? = null
    override suspend fun updateMetadata(metadata: SnippetMetadata): Result<Unit> = Result.success(Unit)
    override suspend fun incrementUsageCount(snippetId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getAllCached(): List<Snippet> = store.value.values.toList()
    override suspend fun getByIdCached(id: String): Snippet? = store.value[id]
}

private class FakeFolderRepository : FolderRepository {
    private val store = MutableStateFlow<Map<String, Folder>>(emptyMap())

    override fun getAll(): Flow<List<Folder>> = store.map { it.values.toList() }
    override fun getRootFolders(): Flow<List<Folder>> = store.map { it.values.filter { f -> f.parentId == null } }
    override fun getSubfolders(parentId: String): Flow<List<Folder>> =
        store.map { it.values.filter { f -> f.parentId == parentId } }
    override suspend fun getById(id: String): Folder? = store.value[id]

    override suspend fun create(folder: Folder): Result<Unit> {
        store.value = store.value + (folder.id to folder)
        return Result.success(Unit)
    }

    override suspend fun update(folder: Folder): Result<Unit> {
        store.value = store.value + (folder.id to folder)
        return Result.success(Unit)
    }

    override suspend fun delete(id: String, moveChildrenToParent: Boolean): Result<Unit> {
        store.value = store.value - id
        return Result.success(Unit)
    }

    override suspend fun move(folderId: String, newParentId: String?): Result<Unit> {
        val f = store.value[folderId] ?: return Result.failure(IllegalArgumentException("not found"))
        store.value = store.value + (folderId to f.copy(parentId = newParentId))
        return Result.success(Unit)
    }
}

class BackupServiceTest {

    private lateinit var tempDir: java.io.File
    private lateinit var snippetRepo: FakeSnippetRepository
    private lateinit var folderRepo: FakeFolderRepository
    private lateinit var storage: FileStorageService
    private lateinit var backupService: BackupService

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("darknote-backup-test-").toFile()
        snippetRepo = FakeSnippetRepository()
        folderRepo = FakeFolderRepository()
        storage = FileStorageService(tempDir)
        backupService = BackupService(snippetRepo, folderRepo, storage)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun folder(id: String, name: String, parentId: String? = null) = Folder(
        id = id, name = name, parentId = parentId, sortOrder = 0, createdAt = 1000L
    )

    private suspend fun createSnippet(
        id: String, title: String, content: String, folderId: String? = null,
        tags: List<String> = emptyList(), isFavorite: Boolean = false
    ): Snippet {
        val snippet = Snippet(
            id = id, title = title, content = content, folderId = folderId,
            tags = tags, isFavorite = isFavorite,
            createdAt = 1000L, modifiedAt = 2000L,
            localPath = "snippets/$id.txt"
        )
        snippetRepo.create(snippet)
        storage.saveSnippetContent(snippet)
        return snippet
    }

    @Test
    fun `export produces valid json with all data`() = runTest {
        folderRepo.create(folder("f1", "Scripts"))
        createSnippet("s1", "backup.sh", "echo hi", folderId = "f1", tags = listOf("bash"), isFavorite = true)

        val json = backupService.exportToJson()

        assertTrue(json.contains("\"backup.sh\""))
        assertTrue(json.contains("\"echo hi\""))
        assertTrue(json.contains("\"Scripts\""))
    }

    @Test
    fun `export then import into empty repos restores everything`() = runTest {
        folderRepo.create(folder("f1", "Scripts"))
        folderRepo.create(folder("f2", "Sub", parentId = "f1"))
        createSnippet("s1", "a.sh", "content A", folderId = "f1", tags = listOf("x", "y"))
        createSnippet("s2", "b.sql", "content B", folderId = "f2", isFavorite = true)

        val json = backupService.exportToJson()

        // Simulate a fresh install: brand new empty repos + storage
        val freshDir = Files.createTempDirectory("darknote-restore-").toFile()
        val freshSnippetRepo = FakeSnippetRepository()
        val freshFolderRepo = FakeFolderRepository()
        val freshStorage = FileStorageService(freshDir)
        val freshService = BackupService(freshSnippetRepo, freshFolderRepo, freshStorage)

        val result = freshService.importFromJson(json)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.foldersImported)
        assertEquals(2, summary.snippetsImported)
        assertTrue(summary.errors.isEmpty())

        assertEquals(folder("f1", "Scripts"), freshFolderRepo.getById("f1"))
        assertEquals("f1", freshFolderRepo.getById("f2")?.parentId)

        val a = freshSnippetRepo.getByIdCached("s1")!!
        assertEquals("a.sh", a.title)
        assertEquals(listOf("x", "y"), a.tags)
        assertEquals("f1", a.folderId)

        val b = freshSnippetRepo.getByIdCached("s2")!!
        assertEquals(true, b.isFavorite)
        assertEquals("f2", b.folderId)

        // Content must be restored via the storage layer too, not just the DB row
        val restoredContentA = freshStorage.loadSnippetContent(a.localPath).getOrNull()
        assertEquals("content A", restoredContentA)
        val restoredContentB = freshStorage.loadSnippetContent(b.localPath).getOrNull()
        assertEquals("content B", restoredContentB)

        freshDir.deleteRecursively()
    }

    @Test
    fun `import is idempotent — running twice does not duplicate`() = runTest {
        folderRepo.create(folder("f1", "Scripts"))
        createSnippet("s1", "a.sh", "content A", folderId = "f1")
        val json = backupService.exportToJson()

        val freshDir = Files.createTempDirectory("darknote-restore2-").toFile()
        val freshSnippetRepo = FakeSnippetRepository()
        val freshFolderRepo = FakeFolderRepository()
        val freshService = BackupService(freshSnippetRepo, freshFolderRepo, FileStorageService(freshDir))

        freshService.importFromJson(json)
        val secondResult = freshService.importFromJson(json).getOrThrow()

        // Second run should update, not duplicate
        assertEquals(0, secondResult.foldersImported)
        assertEquals(1, secondResult.foldersUpdated)
        assertEquals(0, secondResult.snippetsImported)
        assertEquals(1, secondResult.snippetsUpdated)
        assertEquals(1, freshSnippetRepo.getAllCached().size)

        freshDir.deleteRecursively()
    }

    @Test
    fun `import restores nested folder hierarchy regardless of order in file`() = runTest {
        // Deliberately export children before parents in the raw JSON to
        // verify the importer resolves dependency order itself.
        val data = BackupData(
            exportedAt = 1L,
            folders = listOf(
                BackupFolder(id = "child", name = "Child", parentId = "parent", createdAt = 1L),
                BackupFolder(id = "grandchild", name = "Grandchild", parentId = "child", createdAt = 1L),
                BackupFolder(id = "parent", name = "Parent", parentId = null, createdAt = 1L)
            ),
            snippets = emptyList()
        )
        val json = kotlinx.serialization.json.Json.encodeToString(BackupData.serializer(), data)

        val result = backupService.importFromJson(json)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().foldersImported)
        assertTrue(result.getOrThrow().errors.isEmpty())

        assertEquals("parent", folderRepo.getById("child")?.parentId)
        assertEquals("child", folderRepo.getById("grandchild")?.parentId)
    }

    @Test
    fun `import reports error for corrupted json instead of crashing`() = runTest {
        val result = backupService.importFromJson("not valid json {{{")
        assertTrue(result.isFailure)
    }

    @Test
    fun `import handles folder with missing parent gracefully`() = runTest {
        val data = BackupData(
            exportedAt = 1L,
            folders = listOf(
                BackupFolder(id = "orphan", name = "Orphan", parentId = "does-not-exist", createdAt = 1L)
            ),
            snippets = emptyList()
        )
        val json = kotlinx.serialization.json.Json.encodeToString(BackupData.serializer(), data)

        val result = backupService.importFromJson(json)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().foldersImported)
        assertEquals(1, result.getOrThrow().errors.size)
    }

    @Test
    fun `export handles empty library`() = runTest {
        val json = backupService.exportToJson()
        val data = kotlinx.serialization.json.Json.decodeFromString(BackupData.serializer(), json)
        assertTrue(data.folders.isEmpty())
        assertTrue(data.snippets.isEmpty())
    }
}
