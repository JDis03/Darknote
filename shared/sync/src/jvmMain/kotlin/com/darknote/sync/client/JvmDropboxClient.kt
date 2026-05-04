package com.darknote.sync.client

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * JVM/Desktop implementation of DropboxClient using PKCE auth (no secret required).
 * This is the recommended approach for desktop applications.
 */
class JvmDropboxClient(
    private val appKey: String
) : DropboxClient {

    private val config = DbxRequestConfig.newBuilder("darknote/1.0")
        .withAutoRetryEnabled(3)  // 3 retries with exponential backoff
        .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
        .build()
    private val credentialsPath = File(
        System.getProperty("user.home"),
        ".config/darknote/dropbox.properties"
    )

    private var client: DbxClientV2? = null
    private var pkceWebAuth: DbxPKCEWebAuth? = null

    init {
        credentialsPath.parentFile?.mkdirs()
        loadClient()
    }

    override fun isAuthorized(): Boolean = client != null

    override fun getAuthUrl(): String {
        // Use PKCE auth without secret - recommended for desktop apps
        val appInfo = DbxAppInfo(appKey)
        
        pkceWebAuth = DbxPKCEWebAuth(config, appInfo)
        val authRequest = DbxWebAuth.newRequestBuilder()
            .withNoRedirect()
            .withTokenAccessType(com.dropbox.core.TokenAccessType.OFFLINE)
            .build()

        return pkceWebAuth!!.authorize(authRequest)
    }

    override suspend fun finishAuth(code: String): Result<Unit> {
        return try {
            val authFinish: DbxAuthFinish = pkceWebAuth!!.finishFromCode(code)
            // Save credential with auto-refresh support
            saveTokens(
                authFinish.accessToken, 
                authFinish.refreshToken,
                authFinish.expiresAt ?: (System.currentTimeMillis() + 14399 * 1000L) // 4h default
            )
            loadClient()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: String): Result<List<RemoteFile>> {
        val currentClient = client ?: return Result.failure(IllegalStateException("Not authorized"))

        return try {
            withContext(Dispatchers.IO) {
                val result = currentClient.files().listFolder(path)
                val files = result.entries.mapNotNull { entry ->
                    when (entry) {
                        is com.dropbox.core.v2.files.FileMetadata -> {
                            val modifiedTime = entry.clientModified?.time 
                                ?: entry.serverModified?.time 
                                ?: System.currentTimeMillis()
                            RemoteFile(
                                path = entry.pathLower ?: entry.name,
                                name = entry.name,
                                modifiedTime = modifiedTime,
                                size = entry.size
                            )
                        }
                        else -> null // Skip folders
                    }
                }
                Result.success(files)
            }
        } catch (e: Exception) {
            // Check if it's a "folder not found" error
            if (e.message?.contains("path/not_found") == true || 
                e.message?.contains("not_found") == true) {
                // Create the folder and return empty list
                try {
                    currentClient.files().createFolderV2(path)
                    Result.success(emptyList())
                } catch (createError: Exception) {
                    Result.failure(Exception("Folder doesn't exist and couldn't create it: ${createError.message}"))
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): Result<String> {
        val currentClient = client ?: return Result.failure(IllegalStateException("Not authorized"))

        return try {
            withContext(Dispatchers.IO) {
                FileInputStream(localPath).use { inputStream ->
                    val result = currentClient.files().uploadBuilder(remotePath)
                        .withMode(com.dropbox.core.v2.files.WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream)
                    Result.success(result.rev)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit> {
        val currentClient = client ?: return Result.failure(IllegalStateException("Not authorized"))

        return try {
            withContext(Dispatchers.IO) {
                File(localPath).parentFile?.mkdirs()
                FileOutputStream(localPath).use { outputStream ->
                    currentClient.files().download(remotePath).download(outputStream)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        val currentClient = client ?: return Result.failure(IllegalStateException("Not authorized"))

        return try {
            withContext(Dispatchers.IO) {
                currentClient.files().deleteV2(remotePath)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(remotePath: String): Result<RemoteMetadata> {
        val currentClient = client ?: return Result.failure(IllegalStateException("Not authorized"))

        return try {
            withContext(Dispatchers.IO) {
                val metadata = currentClient.files().getMetadata(remotePath)
                if (metadata is com.dropbox.core.v2.files.FileMetadata) {
                    val modifiedTime = metadata.clientModified?.time 
                        ?: metadata.serverModified?.time 
                        ?: System.currentTimeMillis()
                    Result.success(
                        RemoteMetadata(
                            path = metadata.pathLower ?: metadata.name,
                            revision = metadata.rev,
                            modifiedTime = modifiedTime,
                            size = metadata.size
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Not a file"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveTokens(accessToken: String, refreshToken: String?, expiresAt: Long) {
        try {
            val credential = DbxCredential(
                accessToken,
                expiresAt,
                refreshToken,
                appKey
            )
            credentialsPath.writeText(credential.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadClient() {
        if (!credentialsPath.exists()) return

        try {
            val credential = DbxCredential.Reader.readFully(credentialsPath.readText())
            // Client auto-refreshes token when it expires
            client = DbxClientV2(config, credential)
        } catch (e: Exception) {
            e.printStackTrace()
            client = null
        }
    }
}

/**
 * JVM implementation of factory.
 */
actual object DropboxClientFactory {
    // DarkNote official Dropbox app credentials (PKCE - no secret required)
    private const val APP_KEY = "97rske3f4p28pex"

    actual fun create(): DropboxClient {
        return JvmDropboxClient(APP_KEY)
    }
}
