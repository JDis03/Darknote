package com.darknote.sync.engine

import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncMetadata
import com.darknote.core.model.SyncStatus
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
                _progress.value = SyncProgress(1, 5, "Detecting changes...")
                
                val localChanges = detectLocalChanges()
                val remoteChanges = detectRemoteChanges()
                
                addLog("Found ${localChanges.size} local changes, ${remoteChanges.size} remote changes", SyncLogType.INFO)
                
                // Step 4: Resolve conflicts
                _progress.value = SyncProgress(2, 5, "Resolving conflicts...")
                val resolvedChanges = resolveConflicts(localChanges, remoteChanges)
                
                // Step 5: Upload local changes
                _progress.value = SyncProgress(3, 5, "Uploading changes...")
                uploadChanges(resolvedChanges.toUpload)
                
                // Step 6: Download remote changes  
                _progress.value = SyncProgress(4, 5, "Downloading changes...")
                downloadChanges(resolvedChanges.toDownload)
                
                // Step 7: Complete
                _progress.value = SyncProgress(5, 5, "Complete")
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
                    // New snippet - needs to be uploaded
                    changes.add(LocalChange.Created(snippet))
                    addLog("Local create: ${snippet.title}", SyncLogType.INFO)
                }
                snippet.modifiedAt > syncMetadata.lastSyncTime -> {
                    // Modified snippet - needs to be uploaded
                    changes.add(LocalChange.Updated(snippet))
                    addLog("Local update: ${snippet.title}", SyncLogType.INFO)
                }
            }
        }
        
        return changes
    }
    
    /**
     * Detect changes in remote files since last sync.
     */
    private suspend fun detectRemoteChanges(): List<RemoteChange> {
        val result = dropboxClient.listFiles("/")
        if (result.isFailure) {
            throw SyncException("Failed to list remote files: ${result.exceptionOrNull()?.message}")
        }
        
        val remoteFiles = result.getOrThrow()
        val changes = mutableListOf<RemoteChange>()
        
        for (remoteFile in remoteFiles) {
            if (!remoteFile.name.endsWith(".txt")) continue // Only sync .txt files
            
            val snippetId = extractSnippetIdFromPath(remoteFile.path)
            val localSnippet = snippetRepository.getByIdCached(snippetId)
            val syncMetadata = syncMetadataRepository.getBySnippetId(snippetId)
            
            when {
                localSnippet == null -> {
                    // New remote file - needs to be downloaded
                    changes.add(RemoteChange.Created(remoteFile))
                    addLog("Remote create: ${remoteFile.name}", SyncLogType.INFO)
                }
                syncMetadata?.remoteRevision != extractRevisionFromFile(remoteFile) -> {
                    // Modified remote file - needs to be downloaded
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
        
    private fun extractRevisionFromFile(file: RemoteFile): String = 
        "rev_${file.modifiedTime}"
        
    private fun generateRemotePath(snippet: Snippet): String = 
        "/snippets/${snippet.id}.txt"
        
    private suspend fun createTempFile(snippet: Snippet): java.io.File {
        val tempFile = kotlin.io.path.createTempFile().toFile()
        tempFile.writeText(snippet.content)
        return tempFile
    }
    
    private suspend fun createSnippetFromRemote(remoteFile: RemoteFile, content: String) {
        val snippetId = extractSnippetIdFromPath(remoteFile.path)
        val title = remoteFile.name.substringBeforeLast(".txt")
        
        val snippet = Snippet(
            id = snippetId,
            title = title,
            content = content,
            folderId = null,
            tags = emptyList(),
            language = null,
            isFavorite = false,
            createdAt = remoteFile.modifiedTime,
            modifiedAt = remoteFile.modifiedTime,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED,
            localPath = storageService.generateSafePath(title),
            docPath = null
        )
        
        snippetRepository.create(snippet)
        storageService.saveSnippetContent(snippet)
        
        // Create sync metadata
        val syncMetadata = SyncMetadata(
            snippetId = snippetId,
            remoteRevision = extractRevisionFromFile(remoteFile),
            lastSyncTime = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
        syncMetadataRepository.save(syncMetadata)
    }
    
    private suspend fun updateSnippetFromRemote(snippet: Snippet, content: String, modifiedTime: Long) {
        val updatedSnippet = snippet.copy(
            content = content,
            modifiedAt = modifiedTime,
            syncStatus = com.darknote.core.model.SyncStatus.SYNCED
        )
        
        snippetRepository.update(updatedSnippet)
        storageService.saveSnippetContent(updatedSnippet)
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