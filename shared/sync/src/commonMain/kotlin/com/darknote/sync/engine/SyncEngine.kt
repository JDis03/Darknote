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
     * Perform bidirectional sync with automatic retry for transient errors.
     */
    suspend fun sync(): Result<SyncResult> = withRetry {
        if (!dropboxClient.isAuthorized()) {
            _syncState.value = SyncState.AuthRequired
            return@withRetry Result.failure(IllegalStateException("Dropbox not authorized"))
        }

        _syncState.value = SyncState.Syncing
        _syncProgress.value = SyncProgress(total = 0, current = 0)

        // Get remote files - throw underlying exception so withRetry can handle it by type
        val remoteFilesResult = dropboxClient.listFiles("")
        if (remoteFilesResult.isFailure) {
            throw remoteFilesResult.exceptionOrNull()!!
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

        // Upload files - throw on failure so withRetry can handle transient errors
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
                } else {
                    // Re-throw so withRetry can handle by exception type
                    throw result.exceptionOrNull()!!
                }
            }
            _syncProgress.value = SyncProgress(total = total, current = uploaded + downloaded + resolved)
        }

        // Download files - throw on failure so withRetry can handle transient errors
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
            } else {
                // Re-throw so withRetry can handle by exception type
                throw result.exceptionOrNull()!!
            }
            _syncProgress.value = SyncProgress(total = total, current = uploaded + downloaded + resolved)
        }

        _syncState.value = SyncState.Idle
        Result.success(SyncResult(uploaded, downloaded, conflicts.size))
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

    /**
     * Retry wrapper with exponential backoff for transient Dropbox errors.
     * - InvalidAccessTokenException → sets AuthRequired, does NOT retry
     * - NetworkIOException → sets Offline, continues retrying
     * - RetryException → respects backoff hint, continues retrying
     * - Other exceptions → sets Error state, continues retrying
     */
    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) delay(1000L * attempt) // exponential backoff
            try {
                return block()
            } catch (e: com.dropbox.core.InvalidAccessTokenException) {
                _syncState.value = SyncState.AuthRequired
                throw e // Don't retry auth failures
            } catch (e: com.dropbox.core.NetworkIOException) {
                _syncState.value = SyncState.Offline
                lastException = e
                // Continue to retry
            } catch (e: com.dropbox.core.RetryException) {
                val backoff = e.backoffMillis
                delay(backoff)
                lastException = e
            } catch (e: CancellationException) {
                throw e // Don't catch coroutine cancellation
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error", retryable = true)
                lastException = e
            }
        }
        throw lastException ?: Exception("Max retries exceeded")
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
    object Offline : SyncState()        // No internet, working locally
    object AuthRequired : SyncState()   // Token invalid, needs re-auth
    data class Error(val message: String, val retryable: Boolean) : SyncState()
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
