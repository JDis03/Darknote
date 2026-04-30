package com.darknote.sync.client

/**
 * Abstract Dropbox client interface.
 * Platform-specific implementations provide the actual client.
 */
interface DropboxClient {
    /**
     * Check if client is authenticated.
     */
    fun isAuthorized(): Boolean

    /**
     * Get authorization URL for OAuth flow.
     */
    fun getAuthUrl(): String

    /**
     * Complete authentication with code from OAuth flow.
     */
    suspend fun finishAuth(code: String): Result<Unit>

    /**
     * List files in remote folder.
     */
    suspend fun listFiles(path: String = ""): Result<List<RemoteFile>>

    /**
     * Upload file to Dropbox.
     */
    suspend fun uploadFile(localPath: String, remotePath: String): Result<String>

    /**
     * Download file from Dropbox.
     */
    suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit>

    /**
     * Delete remote file.
     */
    suspend fun deleteFile(remotePath: String): Result<Unit>

    /**
     * Get file metadata including revision.
     */
    suspend fun getMetadata(remotePath: String): Result<RemoteMetadata>
}

data class RemoteFile(
    val path: String,
    val name: String,
    val modifiedTime: Long,
    val size: Long
)

data class RemoteMetadata(
    val path: String,
    val revision: String,
    val modifiedTime: Long,
    val size: Long
)

/**
 * Factory for creating platform-specific DropboxClient implementations.
 * Each platform must provide its own implementation.
 */
expect object DropboxClientFactory {
    fun create(): DropboxClient
}
