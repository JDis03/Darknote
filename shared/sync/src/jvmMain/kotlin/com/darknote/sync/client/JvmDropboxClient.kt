package com.darknote.sync.client

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.v2.DbxClientV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * JVM/Desktop implementation of DropboxClient using official SDK.
 */
class JvmDropboxClient(
    private val appKey: String,
    private val appSecret: String? = null
) : DropboxClient {

    private val config = DbxRequestConfig.newBuilder("darknote/1.0").build()
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
        val appInfo = if (appSecret != null) {
            DbxAppInfo(appKey, appSecret)
        } else {
            DbxAppInfo(appKey)
        }

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
            saveTokens(authFinish.accessToken, authFinish.refreshToken)
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
            Result.failure(e)
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

    private fun saveTokens(accessToken: String, refreshToken: String?) {
        val props = Properties()
        props.setProperty("access_token", accessToken)
        if (refreshToken != null) {
            props.setProperty("refresh_token", refreshToken)
        }
        props.store(credentialsPath.outputStream(), "DarkNote Dropbox Credentials")
    }

    private fun loadClient() {
        if (!credentialsPath.exists()) return

        try {
            val props = Properties()
            props.load(credentialsPath.inputStream())
            val accessToken = props.getProperty("access_token")

            client = if (accessToken != null) {
                DbxClientV2(config, accessToken)
            } else {
                null
            }
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
    private const val APP_KEY = "97rske3f4p28pex"

    actual fun create(): DropboxClient {
        return JvmDropboxClient(APP_KEY)
    }
}
