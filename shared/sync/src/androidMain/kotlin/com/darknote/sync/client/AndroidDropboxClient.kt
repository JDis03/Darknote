package com.darknote.sync.client

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Android implementation of DropboxClient using PKCE auth via DbxPKCEWebAuth.
 * Same approach as JvmDropboxClient — no secret required.
 * 
 * SECURITY: Uses EncryptedSharedPreferences to store auth tokens securely.
 */
class AndroidDropboxClient(
    private val context: Context,
    private val appKey: String
) : DropboxClient {

    private val config = DbxRequestConfig.newBuilder("darknote-android/1.0")
        .withAutoRetryEnabled(3)
        .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
        .build()

    // ENCRYPTED SharedPreferences for secure token storage
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "dropbox_auth_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails (rare)
            // In production, you might want to log this or handle differently
            context.getSharedPreferences("dropbox_auth_fallback", Context.MODE_PRIVATE)
        }
    }

    private var client: DbxClientV2? = null
    private var pkceWebAuth: DbxPKCEWebAuth? = null

    init {
        loadClient()
    }

    override fun isAuthorized(): Boolean = client != null

    override fun getAuthUrl(): String {
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
            withContext(Dispatchers.IO) {
                val authFinish: DbxAuthFinish = pkceWebAuth!!.finishFromCode(code)
                val credential = DbxCredential(
                    authFinish.accessToken,
                    authFinish.expiresAt ?: (System.currentTimeMillis() + 14399 * 1000L),
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
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))

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
                    Result.failure(
                        Exception("Folder doesn't exist and couldn't create it: ${createError.message}")
                    )
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): Result<String> {
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                FileInputStream(localPath).use { inputStream ->
                    val metadata = dbxClient.files().uploadBuilder(remotePath)
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
        val dbxClient = client ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            withContext(Dispatchers.IO) {
                File(localPath).parentFile?.mkdirs()
                FileOutputStream(localPath).use { outputStream ->
                    dbxClient.files().download(remotePath).download(outputStream)
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
                        val modTime = metadata.clientModified?.time
                            ?: metadata.serverModified?.time
                            ?: System.currentTimeMillis()
                        Result.success(
                            RemoteMetadata(
                                path = metadata.pathLower ?: metadata.name,
                                revision = metadata.rev,
                                modifiedTime = modTime,
                                size = metadata.size
                            )
                        )
                    }
                    else -> Result.failure(IllegalArgumentException("Not a file: $remotePath"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCredentials(credential: DbxCredential) {
        prefs.edit()
            .putString("access_token", credential.accessToken)
            .putString("refresh_token", credential.refreshToken)
            .putLong("expires_at", credential.expiresAt ?: 0L)
            .apply()
    }

    private fun loadClient() {
        val accessToken = prefs.getString("access_token", null) ?: return
        val refreshToken = prefs.getString("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L).takeIf { it > 0L }

        try {
            val credential = DbxCredential(accessToken, expiresAt, refreshToken, appKey)
            client = DbxClientV2(config, credential)
        } catch (e: Exception) {
            prefs.edit().clear().apply()
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        client = null
    }
}

actual object DropboxClientFactory {
    private const val APP_KEY = "97rske3f4p28pex"

    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun create(): DropboxClient {
        val ctx = context
            ?: throw IllegalStateException("DropboxClientFactory not initialized. Call initialize() first.")
        return AndroidDropboxClient(ctx, APP_KEY)
    }
}
