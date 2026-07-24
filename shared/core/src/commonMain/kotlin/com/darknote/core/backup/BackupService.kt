package com.darknote.core.backup

import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portable backup format for a single folder. Independent of database
 * primary-key implementation details so it survives across app versions
 * and across platforms (Android <-> Desktop).
 */
@Serializable
data class BackupFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long
)

/**
 * Portable backup format for a single snippet, including its full text
 * content (unlike the DB row, which stores content in a separate file).
 */
@Serializable
data class BackupSnippet(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val language: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val modifiedAt: Long
)

/**
 * Top-level backup file format. `formatVersion` is bumped whenever the
 * shape changes so [BackupService.importFromJson] can handle old files.
 */
@Serializable
data class BackupData(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val folders: List<BackupFolder>,
    val snippets: List<BackupSnippet>
)

/** Result of an import operation, for showing a summary to the user. */
data class ImportSummary(
    val foldersImported: Int,
    val foldersUpdated: Int,
    val snippetsImported: Int,
    val snippetsUpdated: Int,
    val errors: List<String>
)

/**
 * Exports/imports the user's entire folder + snippet library to/from a
 * single portable JSON file. Uses only the common repository interfaces,
 * so the exact same code runs on Android and Desktop — a backup made on
 * one platform can be restored on the other.
 *
 * This is deliberately independent of the Dropbox sync engine: it works
 * fully offline and gives the user a file they control directly.
 */
class BackupService(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Serializes the entire library (all folders + all snippets with content) to a JSON string. */
    suspend fun exportToJson(): String {
        val folders = folderRepository.getAll().first().map { it.toBackup() }
        val snippets = snippetRepository.getAllCached().map { snippet ->
            val content = storageService.loadSnippetContent(snippet.localPath)
                .getOrDefault(snippet.content)
            snippet.copy(content = content).toBackup()
        }
        val backup = BackupData(
            exportedAt = System.currentTimeMillis(),
            folders = folders,
            snippets = snippets
        )
        return json.encodeToString(backup)
    }

    /**
     * Restores folders and snippets from a backup JSON string.
     *
     * Upsert semantics: if a folder/snippet with the same [id] already
     * exists locally, it is updated in place; otherwise it's created fresh.
     * This makes imports idempotent — running the same restore twice is safe.
     */
    suspend fun importFromJson(jsonText: String): Result<ImportSummary> {
        return try {
            val backup = json.decodeFromString<BackupData>(jsonText)
            val errors = mutableListOf<String>()

            var foldersCreated = 0
            var foldersUpdated = 0
            // Import folders in an order that satisfies parent_id references:
            // root folders first, then their children, and so on. Each pass
            // only processes folders whose parent already exists *locally*
            // (either pre-existing, or created in an earlier pass of this
            // same import). Anything left after no pass makes progress has
            // a parentId that resolves nowhere — a genuinely broken
            // reference in the backup file — and is reported as an error
            // rather than silently imported with a dangling parent_id.
            val remaining = backup.folders.toMutableList()
            var progressed = true
            while (remaining.isNotEmpty() && progressed) {
                progressed = false
                val iterator = remaining.iterator()
                while (iterator.hasNext()) {
                    val f = iterator.next()
                    val parentReady = f.parentId == null || folderRepository.getById(f.parentId) != null
                    if (parentReady) {
                        val existing = folderRepository.getById(f.id)
                        val folder = Folder(
                            id = f.id,
                            name = f.name,
                            parentId = f.parentId,
                            sortOrder = f.sortOrder,
                            createdAt = f.createdAt
                        )
                        val result = if (existing != null) folderRepository.update(folder) else folderRepository.create(folder)
                        if (result.isSuccess) {
                            if (existing != null) foldersUpdated++ else foldersCreated++
                        } else {
                            errors.add("Folder '${f.name}': ${result.exceptionOrNull()?.message}")
                        }
                        iterator.remove()
                        progressed = true
                    }
                }
            }
            // Any folders left couldn't resolve their parent (broken reference in the backup file)
            remaining.forEach { errors.add("Folder '${it.name}': parent folder not found, skipped") }

            var snippetsCreated = 0
            var snippetsUpdated = 0
            for (s in backup.snippets) {
                val existing = snippetRepository.getByIdCached(s.id)
                val snippet = Snippet(
                    id = s.id,
                    title = s.title,
                    content = s.content,
                    folderId = s.folderId,
                    tags = s.tags,
                    language = s.language,
                    isFavorite = s.isFavorite,
                    createdAt = s.createdAt,
                    modifiedAt = s.modifiedAt,
                    syncStatus = SyncStatus.NOT_SYNCED,
                    localPath = existing?.localPath ?: storageService.generateSafePath(s.title)
                )
                val result = if (existing != null) snippetRepository.update(snippet) else snippetRepository.create(snippet)
                if (result.isSuccess) {
                    storageService.saveSnippetContent(snippet)
                    if (existing != null) snippetsUpdated++ else snippetsCreated++
                } else {
                    errors.add("Snippet '${s.title}': ${result.exceptionOrNull()?.message}")
                }
            }

            Result.success(
                ImportSummary(
                    foldersImported = foldersCreated,
                    foldersUpdated = foldersUpdated,
                    snippetsImported = snippetsCreated,
                    snippetsUpdated = snippetsUpdated,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Folder.toBackup() = BackupFolder(id, name, parentId, sortOrder, createdAt)

    private fun Snippet.toBackup() = BackupSnippet(
        id = id,
        title = title,
        content = content,
        folderId = folderId,
        tags = tags,
        language = language,
        isFavorite = isFavorite,
        createdAt = createdAt,
        modifiedAt = modifiedAt
    )
}
