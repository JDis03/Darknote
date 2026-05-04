package com.darknote.android.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darknote.sync.client.DropboxClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for handling Dropbox authentication in Android.
 */
class AuthViewModel(
    private val dropboxClient: DropboxClient
) : ViewModel() {
    
    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private val _authUrl = mutableStateOf<String?>(null)
    val authUrl: State<String?> = _authUrl

    private val _syncLogs = mutableStateOf<List<SyncLog>>(emptyList())
    val syncLogs: State<List<SyncLog>> = _syncLogs

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val log = SyncLog(
            timestamp = timestamp,
            message = message,
            type = type
        )
        _syncLogs.value = (_syncLogs.value + log).takeLast(50) // Keep last 50 logs
    }

    /**
     * Check if already authorized.
     */
    fun checkAuthStatus() {
        if (dropboxClient.isAuthorized()) {
            _authState.value = AuthState.Authenticated
            addLog("Already authenticated with Dropbox", LogType.SUCCESS)
        } else {
            _authState.value = AuthState.NotAuthenticated
            addLog("Not authenticated", LogType.WARNING)
        }
    }

    /**
     * Generate OAuth URL (Joplin style - no auto-open).
     */
    fun startAuth(context: Context) {
        try {
            val authUrl = dropboxClient.getAuthUrl()
            _authUrl.value = authUrl
            // Keep state as NotAuthenticated to show the URL and code input
            addLog("OAuth URL generated", LogType.INFO)
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to generate auth URL: ${e.message}")
            addLog("Error generating auth URL: ${e.message}", LogType.ERROR)
        }
    }
    
    /**
     * Open OAuth URL in browser manually.
     */
    fun openAuthUrl(context: Context) {
        val url = _authUrl.value
        if (url != null) {
            try {
                // Force external browser, not in-app
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                addLog("Opened browser for authentication", LogType.INFO)
            } catch (e: Exception) {
                addLog("Failed to open browser: ${e.message}", LogType.ERROR)
            }
        }
    }

    /**
     * Complete OAuth authentication with authorization code.
     */
    fun completeAuth(code: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Authorizing
                addLog("Starting token exchange with code: ${code.take(10)}...", LogType.INFO)
                
                // Add timeout handling
                val result = kotlinx.coroutines.withTimeoutOrNull(60000) { // 60 second timeout
                    dropboxClient.finishAuth(code)
                }
                
                if (result == null) {
                    _authState.value = AuthState.Error("Authentication timed out")
                    addLog("Authentication timed out after 60 seconds", LogType.ERROR)
                } else if (result.isSuccess) {
                    _authState.value = AuthState.Authenticated
                    addLog("Authentication successful!", LogType.SUCCESS)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _authState.value = AuthState.Error("Auth failed: $error")
                    addLog("Authentication failed: $error", LogType.ERROR)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Auth error: ${e.message}")
                addLog("Authentication error: ${e.message}", LogType.ERROR)
            }
        }
    }

    /**
     * Test upload functionality.
     */
    fun testUpload() {
        viewModelScope.launch {
            try {
                addLog("Testing upload...", LogType.INFO)
                
                // Create a test file
                val testContent = "Test upload from DarkNote Android - ${Date()}"
                val testFile = java.io.File.createTempFile("darknote_test", ".txt")
                testFile.writeText(testContent)
                
                val result = dropboxClient.uploadFile(
                    localPath = testFile.absolutePath,
                    remotePath = "/darknote/test-snippet-android-${System.currentTimeMillis()}.txt"
                )
                
                testFile.delete() // Clean up
                
                if (result.isSuccess) {
                    addLog("Upload successful! Revision: ${result.getOrNull()}", LogType.SUCCESS)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    addLog("Upload failed: $error", LogType.ERROR)
                }
            } catch (e: Exception) {
                addLog("Upload error: ${e.message}", LogType.ERROR)
            }
        }
    }

    /**
     * Test download functionality.
     */
    fun testDownload() {
        viewModelScope.launch {
            try {
                addLog("Listing files in /darknote...", LogType.INFO)

                val result = dropboxClient.listFiles("/darknote")

                if (result.isSuccess) {
                    val files = result.getOrNull() ?: emptyList()
                    addLog("Found ${files.size} files in Dropbox", LogType.SUCCESS)
                    files.forEach { file ->
                        addLog("  ${file.name} (${file.size} bytes)", LogType.INFO)
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    addLog("List files failed: $error", LogType.ERROR)
                }
            } catch (e: Exception) {
                addLog("List exception: ${e.message}", LogType.ERROR)
            }
        }
    }

    /**
     * Logout from Dropbox.
     */
    fun logout() {
        try {
            // If AndroidDropboxClient has a logout method, call it
            if (dropboxClient is com.darknote.sync.client.AndroidDropboxClient) {
                dropboxClient.logout()
            }
            _authState.value = AuthState.NotAuthenticated
            _authUrl.value = null // Clear auth URL
            addLog("Logged out successfully", LogType.INFO)
        } catch (e: Exception) {
            addLog("Logout error: ${e.message}", LogType.ERROR)
        }
    }

    /**
     * Clear sync logs.
     */
    fun clearLogs() {
        _syncLogs.value = emptyList()
    }
}

/**
 * Authentication states.
 */
sealed class AuthState {
    object Idle : AuthState()
    object NotAuthenticated : AuthState()
    object Authorizing : AuthState()
    object Authenticated : AuthState()
    object Offline : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Sync log entry with timestamp and type.
 */
data class SyncLog(
    val timestamp: String,
    val message: String,
    val type: LogType
)

/**
 * Log types for color coding in UI.
 */
enum class LogType {
    INFO,
    SUCCESS,
    ERROR,
    WARNING
}