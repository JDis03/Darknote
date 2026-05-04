package com.darknote.sync.client

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Android implementation of DropboxClient using official SDK.
 * Uses SharedPreferences for credential storage and CustomTabs for OAuth.
 */
class AndroidDropboxClient(
    private val context: Context,
    private val appKey: String
) : DropboxClient {

    private val config = DbxRequestConfig.newBuilder("darknote-android/1.0")
        .withAutoRetryEnabled(3)
        .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
        .build()

    private val prefs: SharedPreferences = context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE)

    private var client: DbxClientV2? = null

    init {
        loadClient()
    }

    override fun isAuthorized(): Boolean = client != null

    override fun getAuthUrl(): String {
        val redirectUri = "db-$appKey://auth"
        
        // Use proper OAuth2 URL with redirect URI for Android
        return "https://www.dropbox.com/oauth2/authorize?" +
                "client_id=$appKey&" +
                "response_type=code&" +
                "redirect_uri=$redirectUri&" +
                "token_access_type=offline"
    }

    override suspend fun finishAuth(code: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val appInfo = DbxAppInfo(appKey)
                val redirectUri = "db-$appKey://auth"
                
                // Use the proper OAuth2 flow for Android
                val webAuth = com.dropbox.core.DbxWebAuth(config, appInfo)
                val authFinish = webAuth.finishFromCode(code, redirectUri)

                val credential = DbxCredential(
                    authFinish.accessToken,
                    authFinish.expiresAt,
                    authFinish.refreshToken,
                    appKey
                )

                // Save credentials
                saveCredentials(credential)
                
                // Create client
                client = DbxClientV2(config, credential)
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: String): Result<List<RemoteFile>> {
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))
        
        return try {
            withContext(Dispatchers.IO) {
                val result = if (path.isEmpty()) {
                    dbxClient.files().listFolder("")
                } else {
                    dbxClient.files().listFolder(path)
                }

                val files = result.entries.mapNotNull { metadata ->
                    when (metadata) {
                        is FileMetadata -> RemoteFile(
                            path = metadata.pathLower ?: metadata.name,
                            name = metadata.name,
                            modifiedTime = metadata.serverModified?.time ?: 0L,
                            size = metadata.size
                        )
                        else -> null // Skip folders for now
    }
}

                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): Result<String> {
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))
        
        return try {
            withContext(Dispatchers.IO) {
                val localFile = File(localPath)
                if (!localFile.exists()) {
                    return@withContext Result.failure<String>(IllegalArgumentException("Local file not found: $localPath"))
                }

                FileInputStream(localFile).use { inputStream ->
                    val metadata = dbxClient.files().uploadBuilder(remotePath)
                        .withMode(com.dropbox.core.v2.files.WriteMode.OVERWRITE)
                        .withAutorename(false)
                        .uploadAndFinish(inputStream)
                    
                    Result.success(metadata.rev)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit> {
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))
        
        return try {
            withContext(Dispatchers.IO) {
                val localFile = File(localPath)
                localFile.parentFile?.mkdirs()

                FileOutputStream(localFile).use { outputStream ->
                    dbxClient.files().download(remotePath)
                        .download(outputStream)
                }
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))
        
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
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))
        
        return try {
            withContext(Dispatchers.IO) {
                val metadata = dbxClient.files().getMetadata(remotePath)
                
                when (metadata) {
                    is FileMetadata -> {
                        Result.success(
                            RemoteMetadata(
                                path = metadata.pathLower ?: metadata.name,
                                revision = metadata.rev,
                                modifiedTime = metadata.serverModified?.time ?: 0L,
                                size = metadata.size
                            )
                        )
                    }
                    else -> Result.failure(IllegalArgumentException("Path is not a file: $remotePath"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadClient() {
        val accessToken = prefs.getString("access_token", null) ?: return
        val refreshToken = prefs.getString("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L).takeIf { it > 0L }

        try {
            val credential = DbxCredential(accessToken, expiresAt, refreshToken, appKey)
            client = DbxClientV2(config, credential)
        } catch (e: Exception) {
            // Clear invalid credentials
            prefs.edit().clear().apply()
        }
    }

    private fun saveCredentials(credential: DbxCredential) {
        prefs.edit()
            .putString("access_token", credential.accessToken)
            .putString("refresh_token", credential.refreshToken)
            .putLong("expires_at", credential.expiresAt ?: 0L)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
        client = null
    }
}

/**
 * Android implementation of DropboxClientFactory
 */
actual object DropboxClientFactory {
    private const val DEFAULT_APP_KEY = "97rske3f4p28pex" // Same as Desktop
    private var context: Context? = null
    private var appKey: String = DEFAULT_APP_KEY

    fun initialize(context: Context, appKey: String = DEFAULT_APP_KEY) {
        this.context = context.applicationContext
        this.appKey = appKey
    }

    actual fun create(): DropboxClient {
        val ctx = context ?: throw IllegalStateException("DropboxClientFactory not initialized. Call initialize() first.")
        return AndroidDropboxClient(ctx, appKey)
    }
}