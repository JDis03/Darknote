package com.darknote.sync.engine

import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.SnippetRepository
import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.RemoteFile
import com.darknote.sync.client.RemoteMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest

/**
 * Sync engine that coordinates local and remote changes.
 */
class SyncEngine(
    private val snippetRepository: SnippetRepository,
    private val dropboxClient: DropboxClient,
    private val snippetsDir: File = File(System.getProperty("user.home"), ".config/darknote/snippets"),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    init {
        snippetsDir.mkdirs()
    }

    /**
     * Perform bidirectional sync.
     */
    suspend fun sync(): Result<SyncResult> {
        if (!dropboxClient.isAuthorized()) {
            return Result.failure(IllegalStateException("Dropbox not authorized"))
        }

        _syncState.value = SyncState.Syncing
        _syncProgress.value = SyncProgress(total = 0, current = 0)

        return try {
            // Get remote files
            val remoteFilesResult = dropboxClient.listFiles("")
            if (remoteFilesResult.isFailure) {
                _syncState.value = SyncState.Error
                return Result.failure(remoteFilesResult.exceptionOrNull()!!)
            }

            val remoteFiles = remoteFilesResult.getOrThrow()

            // Get local snippets
            val localSnippets = snippetRepository.getAll().first()

            // Calculate changes
            val toUpload = mutableListOf<Snippet>()
            val toDownload = mutableListOf<RemoteFile>()
            val conflicts = mutableListOf<Conflict>()

            // Check local changes
            localSnippets.forEach { snippet ->
                val remoteFile = remoteFiles.find { it.name == File(snippet.localPath).name }

                when {
                    remoteFile == null -> {
                        // New local file - upload
                        toUpload.add(snippet)
                    }
                    snippet.syncStatus == SyncStatus.PENDING_UPLOAD -> {
                        // Modified locally - check for conflict
                        val localHash = calculateFileHash(File(snippetsDir, snippet.localPath))
                        val metadata = dropboxClient.getMetadata(remoteFile.path).getOrNull()

                        if (metadata != null && metadata.revision != snippet.syncStatus.name) {
                            conflicts.add(Conflict(snippet, remoteFile))
                        } else {
                            toUpload.add(snippet)
                        }
                    }
                    else -> {
                        // Check if remote is newer
                        val localFile = File(snippetsDir, snippet.localPath)
                        if (remoteFile.modifiedTime > snippet.modifiedAt) {
                            toDownload.add(remoteFile)
                        }
                    }
                }
            }

            // Check remote files not in local
            remoteFiles.forEach { remoteFile ->
                if (!localSnippets.any { File(it.localPath).name == remoteFile.name }) {
                    toDownload.add(remoteFile)
                }
            }

            // Execute sync
            val total = toUpload.size + toDownload.size + conflicts.size
            _syncProgress.value = SyncProgress(total = total, current = 0)

            var uploaded = 0
            var downloaded = 0
            var resolved = 0

            // Upload files
            toUpload.forEach { snippet ->
                val localFile = File(snippetsDir, snippet.localPath)
                if (localFile.exists()) {
                    val result = dropboxClient.uploadFile(
                        localFile.absolutePath,
                        "/${localFile.name}"
                    )

                    if (result.isSuccess) {
                        snippetRepository.update(snippet.copy(syncStatus = SyncStatus.SYNCED))
                        snippetRepository.updateMetadata(
                            snippet.id,
                            result.getOrThrow(),
                            calculateFileHash(localFile)
                        )
                        uploaded++
                    }
                }
                _syncProgress.value = SyncProgress(total = total, current = uploaded + downloaded + resolved)
            }

            // Download files
            toDownload.forEach { remoteFile ->
                val localPath = File(snippetsDir, remoteFile.name)
                val result = dropboxClient.downloadFile(remoteFile.path, localPath.absolutePath)

                if (result.isSuccess) {
                    // Create snippet record if new
                    val existing = localSnippets.find { File(it.localPath).name == remoteFile.name }
                    if (existing == null) {
                        // Create new snippet from file
                        val content = localPath.readText()
                        val title = remoteFile.name.substringBeforeLast(".", remoteFile.name)
                        val snippet = Snippet(
                            id = generateId(),
                            title = title,
                            content = content,
                            createdAt = remoteFile.modifiedTime,
                            modifiedAt = remoteFile.modifiedTime,
                            syncStatus = SyncStatus.SYNCED,
                            localPath = localPath.absolutePath
                        )
                        snippetRepository.create(snippet)
                    } else {
                        snippetRepository.update(existing.copy(syncStatus = SyncStatus.SYNCED))
                    }
                    downloaded++
                }
                _syncProgress.value = SyncProgress(total = total, current = uploaded + downloaded + resolved)
            }

            _syncState.value = SyncState.Idle
            Result.success(SyncResult(uploaded, downloaded, conflicts.size))

        } catch (e: Exception) {
            _syncState.value = SyncState.Error
            Result.failure(e)
        }
    }

    /**
     * Start automatic sync timer.
     */
    fun startAutoSync(intervalMinutes: Int = 5) {
        coroutineScope.launch {
            while (isActive) {
                sync()
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    /**
     * Stop automatic sync.
     */
    fun stopAutoSync() {
        coroutineScope.cancel()
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(file.readBytes())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}

// Data classes
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Error : SyncState()
}

data class SyncProgress(
    val total: Int = 0,
    val current: Int = 0
) {
    val percentage: Int = if (total > 0) (current * 100 / total) else 0
}

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int
)

data class Conflict(
    val localSnippet: Snippet,
    val remoteFile: RemoteFile
)

// Utility functions for file operations
private fun String.getNameWithoutExtension(): String = substringBeforeLast(".")

private suspend fun SnippetRepository.updateMetadata(snippetId: String, revision: String, hash: String) {
    // Implementation would update metadata
}
