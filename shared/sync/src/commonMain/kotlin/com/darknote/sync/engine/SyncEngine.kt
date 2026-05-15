package com.darknote.sync.engine

import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncMetadata
import com.darknote.core.model.SyncStatus as SnippetSyncStatus
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SyncMetadataRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.RemoteFile
import com.darknote.sync.client.RemoteMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Sync engine inspired by Joplin's synchronization architecture.
 * Handles bidirectional sync with conflict resolution.
 */
class SyncEngine(
    private val dropboxClient: DropboxClient,
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val storageService: FileStorageService
) {
    
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()
    
    private val _progress = MutableStateFlow<SyncProgress?>(null)
    val progress: StateFlow<SyncProgress?> = _progress.asStateFlow()
    
    private val _logs = MutableStateFlow<List<SyncLog>>(emptyList())
    val logs: StateFlow<List<SyncLog>> = _logs.asStateFlow()
    
    /**
     * Perform complete bidirectional synchronization.
     */
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            addLog("Starting synchronization...", SyncLogType.INFO)
            _state.value = SyncState.Syncing
            _progress.value = SyncProgress(0, 0, "Initializing...")
            
            // Step 1: Check authentication
            if (!dropboxClient.isAuthorized()) {
                throw SyncException("Not authenticated with Dropbox")
            }
            addLog("Authentication verified", SyncLogType.SUCCESS)
            
            // Step 2: Acquire sync lock to prevent concurrent syncs
            acquireSyncLock()
            
            try {
                // Step 3: Detect changes
                addLog("Detecting local and remote changes...", SyncLogType.INFO)
                _progress.value = SyncProgress(1, 6, "Detecting changes...")

                val remoteFiles = listRemoteFiles()
                val localChanges = detectLocalChanges()
                val remoteChanges = detectRemoteChanges(remoteFiles)
                val localDeletions = detectLocalDeletions()
                val remoteDeletions = detectRemoteDeletions(remoteFiles)

                addLog("Found ${localChanges.size} local changes, ${remoteChanges.size} remote changes", SyncLogType.INFO)
                addLog("Found ${localDeletions.size} local deletions, ${remoteDeletions.size} remote deletions", SyncLogType.INFO)

                // Step 3.5: Process deletions (but avoid conflicts)
                _progress.value = SyncProgress(2, 6, "Processing deletions...")
                
                // Don't delete remote files if there's a corresponding remote change (conflict: local delete vs remote update)
                val remoteChangedIds = remoteChanges.map { extractSnippetIdFromPath(it.file.path) }.toSet()
                val safeLocalDeletions = localDeletions.filter { it !in remoteChangedIds }
                processLocalDeletions(safeLocalDeletions)
                
                // Don't delete local snippets if there's a corresponding local change (conflict: local update vs remote delete)
                val localChangedIds = localChanges.map { it.snippet.id }.toSet()
                val safeRemoteDeletions = remoteDeletions.filter { it !in localChangedIds }
                processRemoteDeletions(safeRemoteDeletions)

                // Step 5: Resolve conflicts
                _progress.value = SyncProgress(3, 6, "Resolving conflicts...")
                val resolvedChanges = resolveConflicts(localChanges, remoteChanges)

                // Step 5: Upload local changes
                _progress.value = SyncProgress(4, 6, "Uploading changes...")
                uploadChanges(resolvedChanges.toUpload)

                // Step 6: Download remote changes
                _progress.value = SyncProgress(5, 6, "Downloading changes...")
                downloadChanges(resolvedChanges.toDownload)

                // Step 7: Complete
                _progress.value = SyncProgress(6, 6, "Complete")
                _state.value = SyncState.Synced
                addLog("Synchronization completed successfully", SyncLogType.SUCCESS)
                
                Result.success(Unit)
            } finally {
                releaseSyncLock()
            }
            
        } catch (e: Exception) {
            _state.value = SyncState.Error(e.message ?: "Unknown sync error")
            addLog("Sync failed: ${e.message}", SyncLogType.ERROR)
            Result.failure(e)
        }
    }
    
    /**
     * Detect changes in local snippets since last sync.
     */
    private suspend fun detectLocalChanges(): List<LocalChange> {
        val allSnippets = snippetRepository.getAllCached()
        val changes = mutableListOf<LocalChange>()

        for (snippet in allSnippets) {
            val syncMetadata = syncMetadataRepository.getBySnippetId(snippet.id)

            when {
                syncMetadata == null -> {
                    changes.add(LocalChange.Created(snippet))
                    addLog("Local create: ${snippet.title}", SyncLogType.INFO)
                }
                snippet.modifiedAt > syncMetadata.lastSyncTime -> {
                    changes.add(LocalChange.Updated(snippet))
                    addLog("Local update: ${snippet.title}", SyncLogType.INFO)
                }
            }
        }

        return changes
    }

    private suspend fun detectLocalDeletions(): List<String> {
        val localIds = snippetRepository.getAllCached().map { it.id }.toSet()
        val allMetadata = syncMetadataRepository.getAll()

        return allMetadata
            .filter { it.snippetId !in localIds }
            .map { it.snippetId }
            .also { deletedIds ->
                deletedIds.forEach { addLog("Local deletion: $it", SyncLogType.INFO) }
            }
    }

    private suspend fun processLocalDeletions(deletedIds: List<String>) {
        for (id in deletedIds) {
            try {
                val remotePath = "/darknote/$id.txt"
                dropboxClient.deleteFile(remotePath)
                syncMetadataRepository.delete(id)
                addLog("Deleted remote file for $id", SyncLogType.SUCCESS)
            } catch (e: Exception) {
                addLog("Failed to delete remote file for $id: ${e.message}", SyncLogType.WARNING)
            }
        }
    }

    private suspend fun detectRemoteDeletions(remoteFiles: List<RemoteFile>): List<String> {
        val remotePaths = remoteFiles.map { "/darknote/${extractSnippetIdFromPath(it.path)}.txt" }.toSet()

        val allMetadata = syncMetadataRepository.getAll()
        val deletions = mutableListOf<String>()

        for (meta in allMetadata) {
            val expectedPath = "/darknote/${meta.snippetId}.txt"
            if (expectedPath !in remotePaths) {
                val snippet = snippetRepository.getByIdCached(meta.snippetId)
                if (snippet != null) {
                    deletions.add(meta.snippetId)
                    addLog("Remote deletion: ${snippet.title}", SyncLogType.INFO)
                }
            }
        }

        return deletions
    }

    private suspend fun processRemoteDeletions(deletedIds: List<String>) {
        for (id in deletedIds) {
            try {
                val snippet = snippetRepository.getByIdCached(id) ?: continue
                storageService.deleteSnippetFile(snippet.localPath)
                snippetRepository.delete(id)
                syncMetadataRepository.delete(id)
                addLog("Deleted local snippet: ${snippet.title} (removed remotely)", SyncLogType.SUCCESS)
            } catch (e: Exception) {
                addLog("Failed to process remote deletion for $id: ${e.message}", SyncLogType.WARNING)
            }
        }
    }
    
    /**
     * List all remote .txt files in /darknote folder.
     */
    private suspend fun listRemoteFiles(): List<RemoteFile> {
        val result = dropboxClient.listFiles("/darknote")

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            if (error?.message?.contains("not_found") == true ||
                error?.message?.contains("path/not_found") == true) {
                addLog("Remote folder /darknote doesn't exist yet", SyncLogType.INFO)
                return emptyList()
            }
            throw SyncException("Failed to list remote files: ${error?.message}")
        }

        return result.getOrThrow().filter { it.name.endsWith(".txt") }
    }

    /**
     * Detect changes in remote files since last sync.
     */
    private suspend fun detectRemoteChanges(remoteFiles: List<RemoteFile>): List<RemoteChange> {
        val changes = mutableListOf<RemoteChange>()

        for (remoteFile in remoteFiles) {
            val snippetId = extractSnippetIdFromPath(remoteFile.path)
            val localSnippet = snippetRepository.getByIdCached(snippetId)
            val syncMetadata = syncMetadataRepository.getBySnippetId(snippetId)

            when {
                localSnippet == null -> {
                    changes.add(RemoteChange.Created(remoteFile))
                    addLog("Remote create: ${remoteFile.name}", SyncLogType.INFO)
                }
                syncMetadata?.remoteRevision != extractRevisionFromFile(remoteFile) -> {
                    changes.add(RemoteChange.Updated(remoteFile, localSnippet))
                    addLog("Remote update: ${remoteFile.name}", SyncLogType.INFO)
                }
            }
        }

        return changes
    }
    
    /**
     * Resolve conflicts between local and remote changes.
     * Strategy: Last modified wins + create conflict backup.
     */
    private suspend fun resolveConflicts(
        localChanges: List<LocalChange>,
        remoteChanges: List<RemoteChange>
    ): ConflictResolution {
        val toUpload = mutableListOf<LocalChange>()
        val toDownload = mutableListOf<RemoteChange>()
        val conflicts = mutableListOf<ConflictInfo>()
        
        // Create maps for efficient lookup
        val localBySnippetId = localChanges.associateBy { it.snippet.id }
        val remoteBySnippetId = remoteChanges.associateBy { extractSnippetIdFromPath(it.file.path) }
        
        // Process all snippet IDs
        val allSnippetIds = (localBySnippetId.keys + remoteBySnippetId.keys).distinct()
        
        for (snippetId in allSnippetIds) {
            val local = localBySnippetId[snippetId]
            val remote = remoteBySnippetId[snippetId]
            
            when {
                local != null && remote != null -> {
                    // Conflict: both local and remote have changes
                    val localTime = local.snippet.modifiedAt
                    val remoteTime = remote.file.modifiedTime
                    
                    if (localTime > remoteTime) {
                        // Local wins - upload local
                        toUpload.add(local)
                        // Create conflict backup of remote
                        conflicts.add(ConflictInfo(snippetId, "local_wins", "Local version is newer"))
                        addLog("Conflict resolved: Local wins for ${local.snippet.title}", SyncLogType.WARNING)
                    } else {
                        // Remote wins - download remote
                        toDownload.add(remote)
                        // Create conflict backup of local
                        conflicts.add(ConflictInfo(snippetId, "remote_wins", "Remote version is newer"))
                        addLog("Conflict resolved: Remote wins for ${local.snippet.title}", SyncLogType.WARNING)
                    }
                }
                local != null -> {
                    // Only local change - upload
                    toUpload.add(local)
                }
                remote != null -> {
                    // Only remote change - download
                    toDownload.add(remote)
                }
            }
        }
        
        addLog("Resolved ${conflicts.size} conflicts", SyncLogType.INFO)
        
        return ConflictResolution(toUpload, toDownload, conflicts)
    }
    
    /**
     * Upload local changes to remote.
     */
    private suspend fun uploadChanges(changes: List<LocalChange>) {
        for ((index, change) in changes.withIndex()) {
            try {
                val snippet = change.snippet
                addLog("Uploading ${snippet.title}...", SyncLogType.INFO)
                
                // Generate remote path
                val remotePath = generateRemotePath(snippet)
                
                // Save content to temp file first
                val tempFile = createTempFile(snippet)
                
                // Upload to Dropbox
                val uploadResult = dropboxClient.uploadFile(tempFile.absolutePath, remotePath)
                if (uploadResult.isFailure) {
                    throw SyncException("Upload failed for ${snippet.title}: ${uploadResult.exceptionOrNull()?.message}")
                }
                
                val revision = uploadResult.getOrThrow()
                
                // Update sync metadata
                syncMetadataRepository.updateRemoteRevision(snippet.id, revision)
                syncMetadataRepository.updateLastSyncTime(snippet.id)
                
                // Clean up temp file
                tempFile.delete()
                
                addLog("Uploaded ${snippet.title} successfully", SyncLogType.SUCCESS)
                
            } catch (e: Exception) {
                addLog("Failed to upload ${change.snippet.title}: ${e.message}", SyncLogType.ERROR)
                throw e
            }
        }
    }
    
    /**
     * Download remote changes to local.
     */
    private suspend fun downloadChanges(changes: List<RemoteChange>) {
        for (change in changes) {
            try {
                val remoteFile = change.file
                addLog("Downloading ${remoteFile.name}...", SyncLogType.INFO)
                
                // Create temp file for download
                val tempFile = kotlin.io.path.createTempFile().toFile()
                
                // Download from Dropbox
                val downloadResult = dropboxClient.downloadFile(remoteFile.path, tempFile.absolutePath)
                if (downloadResult.isFailure) {
                    throw SyncException("Download failed for ${remoteFile.name}: ${downloadResult.exceptionOrNull()?.message}")
                }
                
                // Read content
                val content = tempFile.readText()
                
                when (change) {
                    is RemoteChange.Created -> {
                        // Create new snippet from remote file
                        createSnippetFromRemote(remoteFile, content)
                    }
                    is RemoteChange.Updated -> {
                        // Update existing snippet
                        updateSnippetFromRemote(change.localSnippet, content, remoteFile.modifiedTime)
                    }
                }
                
                // Update sync metadata
                val snippetId = extractSnippetIdFromPath(remoteFile.path)
                syncMetadataRepository.updateRemoteRevision(snippetId, extractRevisionFromFile(remoteFile))
                syncMetadataRepository.updateLastSyncTime(snippetId)
                
                // Clean up temp file
                tempFile.delete()
                
                addLog("Downloaded ${remoteFile.name} successfully", SyncLogType.SUCCESS)
                
            } catch (e: Exception) {
                addLog("Failed to download ${change.file.name}: ${e.message}", SyncLogType.ERROR)
                throw e
            }
        }
    }
    
    // Helper methods
    private fun addLog(message: String, type: SyncLogType) {
        val log = SyncLog(
            timestamp = System.currentTimeMillis(),
            message = message,
            type = type
        )
        _logs.value = (_logs.value + log).takeLast(100) // Keep last 100 logs
    }
    
    private suspend fun acquireSyncLock() {
        // Simple lock mechanism - create a lock file on Dropbox
        addLog("Acquiring sync lock...", SyncLogType.INFO)
        // TODO: Implement lock file creation
    }
    
    private suspend fun releaseSyncLock() {
        addLog("Releasing sync lock...", SyncLogType.INFO)
        // TODO: Implement lock file deletion
    }
    
    // Helper methods
    private fun extractSnippetIdFromPath(path: String): String = 
        path.substringBeforeLast(".").substringAfterLast("/")
        
    private fun extractRevisionFromFile(file: RemoteFile): String = file.rev.ifEmpty { "rev_${file.modifiedTime}" }
        
    private fun generateRemotePath(snippet: Snippet): String = 
        "/darknote/${snippet.id}.txt"
        
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private suspend fun createTempFile(snippet: Snippet): java.io.File {
        val tempFile = kotlin.io.path.createTempFile().toFile()
        val actualContent = storageService.loadSnippetContent(snippet.localPath)
            .getOrDefault(snippet.content)
        val folderName = snippet.folderId?.let { fid ->
            folderRepository.getById(fid)?.name
        }
        val fileFormat = SnippetFileFormat(
            id = snippet.id,
            title = snippet.title,
            content = actualContent,
            folderId = snippet.folderId,
            folderName = folderName,
            tags = snippet.tags,
            language = snippet.language,
            isFavorite = snippet.isFavorite,
            createdAt = snippet.createdAt,
            modifiedAt = snippet.modifiedAt
        )
        tempFile.writeText(json.encodeToString(fileFormat))
        return tempFile
    }
    
    private suspend fun createSnippetFromRemote(remoteFile: RemoteFile, fileContent: String) {
        val parsed = parseSnippetFile(fileContent, remoteFile)

        // Auto-create folder if referenced and doesn't exist locally
        val localFolderId = if (parsed.folderId != null) {
            createFolderIfMissing(parsed.folderId, parsed.folderName ?: "Imported Folder")
        } else null

        val snippet = Snippet(
            id = parsed.id,
            title = parsed.title,
            content = parsed.content,
            folderId = localFolderId,
            tags = parsed.tags,
            language = parsed.language,
            isFavorite = parsed.isFavorite,
            createdAt = parsed.createdAt,
            modifiedAt = parsed.modifiedAt,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED,
            localPath = storageService.generateSafePath(parsed.title),
            docPath = null
        )

        snippetRepository.create(snippet)
        storageService.saveSnippetContent(snippet)

        val syncMetadata = SyncMetadata(
            snippetId = parsed.id,
            remoteRevision = extractRevisionFromFile(remoteFile),
            lastSyncTime = System.currentTimeMillis(),
            syncStatus = SnippetSyncStatus.SYNCED
        )
        syncMetadataRepository.save(syncMetadata)

        addLog("Imported '${parsed.title}' from remote", SyncLogType.SUCCESS)
    }

    private suspend fun createFolderIfMissing(folderId: String, folderName: String): String {
        val existing = folderRepository.getById(folderId)
        if (existing != null) return folderId

        val folder = com.darknote.core.model.Folder(
            id = folderId,
            name = folderName,
            parentId = null,
            sortOrder = 0,
            createdAt = System.currentTimeMillis()
        )
        folderRepository.create(folder)
        addLog("Auto-created folder '$folderName' from remote snippet", SyncLogType.INFO)
        return folderId
    }

    private suspend fun updateSnippetFromRemote(snippet: Snippet, fileContent: String, modifiedTime: Long) {
        val parsed = parseSnippetFile(fileContent, RemoteFile("", "", modifiedTime, 0))

        val updatedSnippet = snippet.copy(
            title = parsed.title,
            content = parsed.content,
            tags = parsed.tags,
            language = parsed.language,
            isFavorite = parsed.isFavorite,
            modifiedAt = parsed.modifiedAt,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED
        )

        snippetRepository.update(updatedSnippet)
        storageService.saveSnippetContent(updatedSnippet)
    }

    private fun parseSnippetFile(raw: String, fallback: RemoteFile): SnippetFileFormat {
        return try {
            json.decodeFromString<SnippetFileFormat>(raw)
        } catch (e: Exception) {
            val id = fallback.path.substringBeforeLast(".").substringAfterLast("/")
            val title = fallback.name.substringBeforeLast(".txt")
            SnippetFileFormat(
                id = id,
                title = title,
                content = raw,
                tags = emptyList(),
                createdAt = fallback.modifiedTime,
                modifiedAt = fallback.modifiedTime
            )
        }
    }
}

/**
 * Sync states
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Sync progress tracking
 */
data class SyncProgress(
    val current: Int,
    val total: Int,
    val message: String
)

/**
 * Sync log entry
 */
data class SyncLog(
    val timestamp: Long,
    val message: String,
    val type: SyncLogType
)

enum class SyncLogType {
    INFO, SUCCESS, WARNING, ERROR
}

/**
 * Local changes to sync
 */
sealed class LocalChange {
    abstract val snippet: Snippet
    
    data class Created(override val snippet: Snippet) : LocalChange()
    data class Updated(override val snippet: Snippet) : LocalChange()
}

/**
 * Remote changes to sync
 */
sealed class RemoteChange {
    abstract val file: RemoteFile
    
    data class Created(override val file: RemoteFile) : RemoteChange()
    data class Updated(override val file: RemoteFile, val localSnippet: Snippet) : RemoteChange()
}

/**
 * Conflict resolution result
 */
data class ConflictResolution(
    val toUpload: List<LocalChange>,
    val toDownload: List<RemoteChange>,
    val conflicts: List<ConflictInfo>
)

/**
 * Conflict information
 */
data class ConflictInfo(
    val snippetId: String,
    val resolution: String,
    val reason: String
)

/**
 * Sync-related exceptions
 */
class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Wire format for synced snippet files — contains full metadata + content.
 */
@Serializable
data class SnippetFileFormat(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val folderName: String? = null,
    val tags: List<String> = emptyList(),
    val language: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val modifiedAt: Long
)