package com.darknote.sync.client

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.prefs.Preferences

class JvmDropboxClient(private val appKey: String) : DropboxClient {

    private val config = DbxRequestConfig.newBuilder("darknote-desktop/1.0")
        .withAutoRetryEnabled(3)
        .build()

    // Use Java Preferences API for secure storage
    private val prefs = Preferences.userNodeForPackage(JvmDropboxClient::class.java)

    private var client: DbxClientV2? = null
    private var currentCredential: DbxCredential? = null
    private var pkceWebAuth: DbxPKCEWebAuth? = null

    init {
        loadClient()
    }

    override fun isAuthorized(): Boolean {
        if (client == null) {
            loadClient()
        }
        
        val credential = currentCredential ?: return false
        val expiresAt = credential.expiresAt
        
        if (expiresAt == null || credential.refreshToken != null) {
            return client != null
        }
        
        return expiresAt > System.currentTimeMillis()
    }

    override fun getAuthUrl(): String {
        val appInfo = DbxAppInfo(appKey)
        pkceWebAuth = DbxPKCEWebAuth(config, appInfo)
        val authRequest = DbxWebAuth.newRequestBuilder()
            .withNoRedirect()
            .withTokenAccessType(com.dropbox.core.TokenAccessType.OFFLINE)
            .build()
        
        return pkceWebAuth!!.authorize(authRequest)
    }

    override suspend fun finishAuth(authCode: String): Result<Unit> {
        val webAuth = pkceWebAuth ?: return Result.failure(IllegalStateException("Auth not started"))
        
        return try {
            withContext(Dispatchers.IO) {
                val authFinish: DbxAuthFinish = webAuth.finishFromCode(authCode)
                val credential = DbxCredential(
                    authFinish.accessToken,
                    authFinish.expiresAt,
                    authFinish.refreshToken,
                    appKey
                )
                
                saveCredentials(credential)
                client = DbxClientV2(config, credential)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: String): Result<List<RemoteFile>> {
        val dbxClient = ensureClient() ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                val result = dbxClient.files().listFolder(path)
                val files = result.entries.mapNotNull { entry ->
                    when (entry) {
                        is FileMetadata -> RemoteFile(
                            path = entry.pathLower ?: entry.name,
                            name = entry.name,
                            modifiedTime = entry.clientModified?.time
                                ?: entry.serverModified?.time
                                ?: System.currentTimeMillis(),
                            size = entry.size,
                            rev = entry.rev
                        )
                        else -> null
                    }
                }
                Result.success(files)
            }
        } catch (e: Exception) {
            if (e.message?.contains("path/not_found") == true ||
                e.message?.contains("not_found") == true
            ) {
                try {
                    dbxClient.files().createFolderV2(path)
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
        val dbxClient = ensureClient() ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                FileInputStream(localPath).use { inputStream ->
                    val metadata = dbxClient.files()
                        .uploadBuilder(remotePath)
                        .withMode(com.dropbox.core.v2.files.WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream)
                    Result.success(metadata.rev)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit> {
        val dbxClient = ensureClient() ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                val localFile = File(localPath)
                localFile.parentFile?.mkdirs()
                
                FileOutputStream(localFile).use { outputStream ->
                    dbxClient.files().download(remotePath).download(outputStream)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        val dbxClient = ensureClient() ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                dbxClient.files().deleteV2(remotePath)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(remotePath: String): Result<RemoteMetadata> {
        val dbxClient = ensureClient() ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                val metadata = dbxClient.files().getMetadata(remotePath) as? FileMetadata
                    ?: return@withContext Result.failure(Exception("Not a file"))
                
                Result.success(RemoteMetadata(
                    path = metadata.pathLower ?: remotePath,
                    revision = metadata.rev,
                    modifiedTime = metadata.serverModified?.time ?: System.currentTimeMillis(),
                    size = metadata.size
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCredentials(credential: DbxCredential) {
        currentCredential = credential
        prefs.put("access_token", credential.accessToken)
        credential.refreshToken?.let { prefs.put("refresh_token", it) }
        credential.expiresAt?.let { prefs.putLong("expires_at", it) }
    }

    private fun loadClient() {
        val accessToken = prefs.get("access_token", null) ?: return
        val refreshToken = prefs.get("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L).takeIf { it > 0L }

        try {
            val credential = DbxCredential(accessToken, expiresAt, refreshToken, appKey)
            currentCredential = credential
            client = DbxClientV2(config, credential)
        } catch (e: Exception) {
            prefs.clear()
            currentCredential = null
        }
    }

    private suspend fun ensureClient(): DbxClientV2? {
        if (client == null) return null

        val credential = currentCredential ?: return null
        val expiresAt = credential.expiresAt ?: return client

        if (expiresAt < System.currentTimeMillis() + 60_000L) {
            return try {
                withContext(Dispatchers.IO) {
                    val refreshed = client!!.refreshAccessToken()
                    saveCredentials(DbxCredential(
                        refreshed.accessToken,
                        refreshed.expiresAt,
                        credential.refreshToken,
                        appKey
                    ))
                    client = DbxClientV2(config, currentCredential!!)
                    client
                }
            } catch (e: Exception) {
                logout()
                null
            }
        }

        return client
    }

    fun logout() {
        prefs.clear()
        client = null
        currentCredential = null
    }
}

actual object DropboxClientFactory {
    private const val APP_KEY = "97rske3f4p28pex"

    actual fun create(): DropboxClient {
        return JvmDropboxClient(APP_KEY)
    }
}
