package com.darknote.sync.client

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxPKCEWebAuth
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
 * Token response data class for parsing Dropbox API response
 */
private data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String? = null
)

/**
 * Android implementation of DropboxClient using official SDK.
 * Uses SharedPreferences for credential storage and CustomTabs for OAuth.
 */
class AndroidDropboxClient(
    private val context: Context,
    private val appKey: String,
    private val appSecret: String? = null // Optional for public apps
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
        // Follow Joplin's simple approach - no redirect URI, user copies code manually
        return "https://www.dropbox.com/oauth2/authorize?" +
                "response_type=code&" +
                "client_id=$appKey"
    }

    override suspend fun finishAuth(code: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                println("[AndroidDropboxClient] Starting token exchange for code: ${code.take(10)}...")
                
                // Follow Joplin's approach - direct token exchange
                val response = executeTokenExchange(code)
                
                println("[AndroidDropboxClient] Token exchange successful, access_token: ${response.access_token.take(10)}...")
                
                val credential = DbxCredential(
                    response.access_token,
                    null, // Long-lived token, no expiration
                    response.refresh_token,
                    appKey
                )
                
                saveCredentials(credential)
                client = DbxClientV2(config, credential)
                
                println("[AndroidDropboxClient] Auth completed successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            println("[AndroidDropboxClient] finishAuth failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun executeTokenExchange(authCode: String): TokenResponse {
        val tokenUrl = "https://api.dropboxapi.com/oauth2/token"
        
        println("[AndroidDropboxClient] Executing token exchange to: $tokenUrl")
        println("[AndroidDropboxClient] Auth code: ${authCode.take(10)}...")
        
        val formBodyBuilder = okhttp3.FormBody.Builder()
            .add("code", authCode)
            .add("grant_type", "authorization_code")
            .add("client_id", appKey)
            
        // Add client_secret if available (following Joplin's approach)
        if (appSecret != null) {
            formBodyBuilder.add("client_secret", appSecret)
            println("[AndroidDropboxClient] Using client_secret: ${appSecret.take(5)}...")
        } else {
            println("[AndroidDropboxClient] No client_secret provided (public app)")
        }
        
        val formBody = formBodyBuilder.build()
        
        val request = okhttp3.Request.Builder()
            .url(tokenUrl)
            .post(formBody)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()
        
        println("[AndroidDropboxClient] Making HTTP request...")
        
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val response = okHttpClient.newCall(request).execute()
        
        val responseBody = response.body?.string()
        
        println("[AndroidDropboxClient] HTTP Response: ${response.code}")
        println("[AndroidDropboxClient] Response body: ${responseBody?.take(200)}...")
        
        if (!response.isSuccessful) {
            throw Exception("Token exchange failed: ${response.code} - $responseBody")
        }
        
        return parseTokenResponse(responseBody!!)
    }
    
    private fun parseTokenResponse(json: String): TokenResponse {
        // Simple JSON parsing following Joplin's approach
        val accessToken = extractJsonField(json, "access_token")
            ?: throw Exception("No access_token in response")
        val refreshToken = extractJsonField(json, "refresh_token")
        val tokenType = extractJsonField(json, "token_type") ?: "bearer"
        
        return TokenResponse(accessToken, tokenType, refreshToken)
    }
    
    private fun extractJsonField(json: String, fieldName: String): String? {
        val pattern = "\"$fieldName\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
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
    // Use Joplin's dev credentials for now (working OAuth setup)
    private const val DEFAULT_APP_KEY = "cx9li9ur8taq1z7" // Joplin dev
    private const val DEFAULT_APP_SECRET = "i8f9a1mvx3bijrt" // Joplin dev
    
    private var context: Context? = null
    private var appKey: String = DEFAULT_APP_KEY
    private var appSecret: String = DEFAULT_APP_SECRET

    fun initialize(context: Context, appKey: String = DEFAULT_APP_KEY, appSecret: String = DEFAULT_APP_SECRET) {
        this.context = context.applicationContext
        this.appKey = appKey
        this.appSecret = appSecret
    }

    actual fun create(): DropboxClient {
        val ctx = context ?: throw IllegalStateException("DropboxClientFactory not initialized. Call initialize() first.")
        return AndroidDropboxClient(ctx, appKey, appSecret)
    }
}