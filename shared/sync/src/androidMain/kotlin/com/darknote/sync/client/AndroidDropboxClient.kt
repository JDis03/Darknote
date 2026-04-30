package com.darknote.sync.client

/**
 * Android implementation of DropboxClient.
 * Uses Dropbox REST API with Ktor HTTP client.
 */
class AndroidDropboxClient : DropboxClient {
    override fun isAuthorized(): Boolean {
        // TODO: Check if access token exists in secure storage
        return false
    }

    override fun getAuthUrl(): String {
        // TODO: Implement OAuth flow
        return "https://www.dropbox.com/oauth2/authorize"
    }

    override suspend fun finishAuth(code: String): Result<Unit> {
        // TODO: Exchange code for tokens
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }

    override suspend fun listFiles(path: String): Result<List<RemoteFile>> {
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): Result<String> {
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit> {
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }

    override suspend fun getMetadata(remotePath: String): Result<RemoteMetadata> {
        return Result.failure(NotImplementedError("Android DropboxClient not yet implemented"))
    }
}

/**
 * Android implementation of factory.
 */
actual object DropboxClientFactory {
    actual fun create(): DropboxClient {
        return AndroidDropboxClient()
    }
}
